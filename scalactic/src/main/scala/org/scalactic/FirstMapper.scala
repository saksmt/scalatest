/*
 * Copyright 2001-2013 Artima, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scalactic

import scala.util.Try
import scala.util.Success
import scala.util.Failure
import scala.util.control.NonFatal
import scala.collection.GenTraversableOnce
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.Builder

/**
 * @param value the underlying <code>Else</code> value wrapped in this <code>FirstMapper</code>.
 */
class FirstMapper[+B,+W] private[scalactic] (val value: B Else W) extends AnyVal with Serializable { thisFirstMapper =>

  /**
   * Indicates whether the <code>Else</code> underlying this </code>FirstMapper</code> is a <code>First</code>
   *
   * @return true if the underlying <code>Else</code> is a <code>First</code>, <code>false</code> if it is a <code>Second</code>.
   */
  def isFirst: Boolean = thisFirstMapper.value.isFirst

  /**
   * Indicates whether the <code>Else</code> underlying this </code>FirstMapper</code> is a <code>Second</code>
   *
   * @return true if the underlying <code>Else</code> is a <code>Second</code>, <code>false</code> if it is a <code>First</code>.
   */
  def isSecond: Boolean = thisFirstMapper.value.isSecond

  /**
   * Applies the given function to the value contained in the underlying <code>Else</code> if it is a <code>First</code>, and 
   * returns a new <code>FirstMapper</code> wrapping a new <code>First</code> containing the result of the function application;
   * or returns <code>this</code> if the underlying <code>Else</code> is a <code>Second</code>.
   *
   * @param f the function to apply
   * @return if the underlying <code>Else</code> is a <code>First</code>, the result of applying the given function to the contained
   *         value wrapped in a <code>First</code> wrapped in an <code>FirstMapper</code>,
   *         else this <code>FirstMapper</code> (already containing a <code>Second</code>)
   */
  def map[C](f: B => C): FirstMapper[C, W] =
    thisFirstMapper.value match {
      case First(b) => new FirstMapper(First(f(b)))
      case w: Second[W] => new FirstMapper(w)
    }

  /**
   * Applies the given function to the value in the <code>Else</code> underlying this <code>FirstMapper</code> if the underlying
   * <code>Else</code> is a <code>Second</code>, returning an <code>FirstMapper</code> wrapping a <code>First</code> containing
   * the result of the function application, or returns
   * <code>this</code> if the underlying <code>Else</code> is already a <code>First</code>.
   *
   * @param f the function to apply
   * @return if the underlying <code>Else</code> is a <code>Second</code>, the result of applying the given function to the
   *         contained value wrapped in a <code>First</code> wrapped in an <code>FirstMapper</code>,
   *         else this <code>FirstMapper</code> (already containing a <code>First</code>)
   */
  def recover[C >: B](f: W => C): FirstMapper[C, W] =
    thisFirstMapper.value match {
      case Second(w) => new FirstMapper(First(f(w)))
      case b: First[B] => new FirstMapper(b)
    }

  /**
   * Applies the given function to the value in the <code>Else</code> underlying this <code>FirstMapper</code>'s value if the
   * underlying <code>Else</code> is a <code>Second</code>, returning the result, or returns
   * <code>this</code> if the underlying <code>Else</code> is a <code>First</code>.
   *
   * @param f the function to apply
   * @return if the underlying <code>Else</code> is a <code>Second</code>, the result of applying the given function to the
   *         contained value, else this <code>FirstMapper</code> (already containing a <code>First</code>)
   */
  def recoverWith[C >: B, X](f: W => FirstMapper[C, X]): FirstMapper[C, X] =
    thisFirstMapper.value match {
      case Second(w) => f(w)
      case b: First[B] => new FirstMapper(b)
    }

  /**
   * Applies the given function f to the contained value if the <code>Else</code> underlying this </code>FirstMapper</code> is a <code>First</code>; does nothing if the underlying <code>Else</code>
   * is a <code>Second</code>.
   *
   * @param f the function to apply
   */
  def foreach(f: B => Unit): Unit =
    thisFirstMapper.value match {
      case First(b) => f(b)
      case _ => ()
    }

  /**
   * Applies the given function to the value contained in the <code>Else</code> underlying this <code>FirstMapper</code> if it is a <code>First</code>,
   * returning the result;
   * or returns <code>this</code> if the underlying <code>Else</code> is a <code>Second</code>.
   *
   * @param f the function to apply
   * @return if the underlying <code>Else</code> is a <code>First</code>, the result of applying the given function to the value contained in the
   *         underlying <code>First</code>,
   *         else this <code>FirstMapper</code> (already containing a <code>Second</code>)
   */
  def flatMap[C, X >: W](f: B => FirstMapper[C, X]): FirstMapper[C, X] =
    thisFirstMapper.value match {
      case First(b) => f(b)
      case w: Second[W] => new FirstMapper(w)
    }

  /**
   * Returns this <code>FirstMapper</code> if either 1) the underlying <code>Else</code> is a <code>Second</code> or 2) it is a <code>First</code> and applying the validation function <code>f</code> to the
   * <code>First</code>'s value returns <code>Pass</code>; otherwise, 
   * returns a new <code>FirstMapper</code> wrapping a <code>Second</code> containing the error value contained in the <code>Fail</code> resulting from applying the validation
   * function <code>f</code> to this <code>FirstMapper</code>'s underlying <code>First</code> value.
   *
   * <p>
   * For examples of <code>filter</code> used in <code>for</code> expressions, see the main documentation for trait
   * <a href="Validation.html"><code>Validation</code></a>.
   * </p>
   *
   * @param f the validation function to apply
   * @return an <code>FirstMapper</code> wrapping a <code>First</code> if the underlying <code>Else</code> is a <code>First</code> that passes the validation function, else an <code>FirstMapper</code> wrapping a <code>Second</code>.
   */
  def filter[X >: W](f: B => Validation[X]): FirstMapper[B, X] =
    thisFirstMapper.value match {
      case First(b) =>
        f(b) match {
          case Pass => thisFirstMapper
          case Fail(x) => new FirstMapper(Second(x))
        }
      case _ => thisFirstMapper
    }

  // TODO: What should we do about withFilter. First question for the hackathon.
  /**
   * Currently just forwards to </code>filter</code>, and therefore, returns the same result.
   */
  def withFilter[X >: W](f: B => Validation[X]): FirstMapper[B, X] = filter(f)

  /**
   * Returns <code>true</code> if the <code>Else</code> underlying this </code>FirstMapper</code> is a <code>First</code> and the predicate <code>p</code> returns true when applied to the underlying <code>First</code>'s value.
   *
   * <p>
   * Note: The <code>exists</code> method will return the same result as <code>forall</code> if the underlying <code>Else</code> is a <code>First</code>, but the opposite
   * result if the underlying <code>Else</code> is a <code>Second</code>.
   * </p>
   *
   * @param p the predicate to apply to the <code>First</code> value, if the underlying <code>Else</code> is a <code>First</code>
   * @return the result of applying the passed predicate <code>p</code> to the <code>First</code> value, if this is a <code>First</code>, else <code>false</code>
   */
  def exists(p: B => Boolean): Boolean =
    thisFirstMapper.value match {
      case First(b) => p(b)
      case _ => false
    }

  /**
   * Returns <code>true</code> if either the <code>Else</code> underlying this </code>FirstMapper</code> is a <code>Second</code> or if the predicate <code>p</code> returns <code>true</code> when applied
   * to the underlying <code>First</code>'s value.
   *
   * <p>
   * Note: The <code>forall</code> method will return the same result as <code>exists</code> if the underlying <code>Else</code> is a <code>First</code>, but the opposite
   * result if the underlying <code>Else</code> is a <code>Second</code>.
   * </p>
   *
   * @param p the predicate to apply to the <code>First</code> value, if the underlying <code>Else</code> is a <code>First</code>
   * @return the result of applying the passed predicate <code>p</code> to the <code>First</code> value, if this is a <code>First</code>, else <code>true</code>
   */
  def forall(p: B => Boolean): Boolean =
    thisFirstMapper.value match {
      case First(b) => p(b)
      case _ => true
    }

  /**
   * Returns, if the <code>Else</code> underlying this </code>FirstMapper</code> is <code>First</code>, the <code>First</code>'s value; otherwise returns the result of evaluating <code>default</code>. 
   *
   * @param default the default expression to evaluate if the underlying <code>Else</code> is a <code>Second</code>
   * @return the contained value, if the underlying <code>Else</code> is a <code>First</code>, else the result of evaluating the given <code>default</code>
   */
  def getOrElse[C >: B](default: => C): C =
    thisFirstMapper.value match {
      case First(b) => b
      case _ => default
    }

  /**
   * Returns this <code>FirstMapper</code> if the underlying <code>Else</code> is a <code>First</code>, otherwise returns the result of evaluating the passed <code>alternative</code>.
   *
   * @param alternative the alternative by-name to evaluate if the underlying <code>Else</code> is a <code>Second</code>
   * @return this <code>FirstMapper</code>, if the underlying <code>Else</code> is a <code>First</code>, else the result of evaluating <code>alternative</code>
   */
  def orElse[C >: B, X >: W](alternative: => FirstMapper[C, X]): FirstMapper[C, X] =
    if (isFirst) thisFirstMapper else alternative

  /**
   * Returns a <code>Some</code> containing the <code>First</code> value, if the <code>Else</code> underlying this <code>FirstMapper</code>
   * is a <code>First</code>, else <code>None</code>.
   *
   * @return the contained <code>First</code> value wrapped in a <code>Some</code>, if the underlying <code>Else</code> is a <code>First</code>;
   * <code>None</code> if the underlying <code>Else</code> is a <code>Second</code>.
   */
  def toOption: Option[B] =
    thisFirstMapper.value match {
      case First(b) => Some(b)
      case _ => None
    }

  /**
   * Returns an immutable <code>IndexedSeq</code> containing the <code>First</code> value, if the <code>Else</code> underlying this </code>FirstMapper</code> is a <code>First</code>, else an empty
   * immutable <code>IndexedSeq</code>.
   *
   * @return the contained <code>First</code> value in a lone-element <code>Seq</code> if the underlying <code>Else</code> is a <code>First</code>; an empty <code>Seq</code> if
   *     the underlying <code>Else</code> is a <code>Second</code>.
   */
  def toSeq: scala.collection.immutable.IndexedSeq[B] =
    thisFirstMapper.value match {
      case First(b) => Vector(b)
      case _ => Vector.empty
    }

  /**
   * Returns an <code>Either</code>: a <code>Right</code> containing the <code>First</code> value, if this is a <code>First</code>; or a <code>Left</code>
   * containing the <code>Second</code> value, if this is a <code>Second</code>.
   *
   * <p>
   * Note that values effectively &ldquo;switch sides&rdquo; when converting an <code>FirstMapper</code> to an <code>Either</code>. If the type of the
   * <code>FirstMapper</code> on which you invoke <code>toEither</code> is <code>FirstMapper[Int, Double]</code>, for example, the result will be an
   * <code>Either[Double, Int]</code>. The reason is that the convention for <code>Either</code> is that <code>Left</code> is used for &ldquo;unexpected&rdquo;
   * or &ldquo;error&rdquo; values and <code>Right</code> is used for &ldquo;expected&rdquo; or &ldquo;successful&rdquo; ones.
   * </p>
   *
   * @return if the underlying <code>Else</code> is a <code>First</code>, the <code>First</code> value wrapped in a <code>Right</code>, else the
   *         underlying <code>Second</code> value, wrapped in a <code>Left</code>.
   */
  def toEither: Either[W, B] =
    thisFirstMapper.value match {
      case First(b) => Right(b)
      case Second(w) => Left(w)
    }

  /**
   * Returns an <code>Or</code>: a <code>Good</code> containing the <code>First</code> value, if this is a <code>First</code>; or a <code>Bad</code>
   * containing the <code>Second</code> value, if this is a <code>Second</code>.
   *
   * <p>
   * Note that values effectively &ldquo;stay on the same sides&rdquo; when converting an <code>Secondern</code> to an <code>Or</code>. If the type of the
   * <code>Secondern</code> on which you invoke <code>toOr</code> is <code>Secondern[Double, Int]</code>, for example, the result will be an
   * <code>Or[Double, Int]</code>. The reason is that the convention for <code>Or</code> is that <code>Bad</code> (the right side) is used for &ldquo;unexpected&rdquo;
   * or &ldquo;error&rdquo; values and <code>Good</code> (the left side) is used for &ldquo;expected&rdquo; or &ldquo;successful&rdquo; ones.
   * </p>
   *
   * @return if the underlying <code>Else</code> is a <code>First</code>, the <code>First</code> value wrapped in a <code>Good</code>, else the
   *         underlying <code>Second</code> value, wrapped in a <code>Bad</code>.
   */
  def toOr: B Or W =
    thisFirstMapper.value match {
      case First(b) => Good(b)
      case Second(w) => Bad(w)
    }

  /**
   * Returns a <code>Try</code>: a <code>Success</code> containing the
   * <code>First</code> value, if this is a <code>First</code>; or a <code>Failure</code>
   * containing the <code>Second</code> value, if this is a <code>Second</code>.
   *
   * <p>
   * Note: This method can only be called if the <code>Second</code> type of this <code>FirstMapper</code> is a subclass
   * of <code>Throwable</code> (including <code>Throwable</code> itself).
   * </p>
   *
   * @return the underlying <code>First</code> value, wrapped in a <code>Success</code>, if the underlying <code>Else</code> is a <code>First</code>; else
   * the underlying <code>Second</code> value, wrapped in a <code>Failure</code>.
   */
  def toTry(implicit ev: W <:< Throwable): Try[B] =
    thisFirstMapper.value match {
      case First(b) => Success(b)
      case Second(w) => Failure(w)
    }

  /**
   * Returns a <code>FirstMapper</code> with the <code>First</code> and <code>Second</code> types swapped: <code>Second</code> becomes <code>First</code> and <code>First</code>
   * becomes <code>Second</code>.
   *
   * @return if the underlying <code>Else</code> is a <code>First</code>, its <code>First</code> value wrapped in a <code>Second</code> then wrapped in an
   *     <code>FirstMapper</code>; if the underlying <code>Else</code> is
   *     a <code>Second</code>, its <code>Second</code> value wrapped in a <code>First</code> then wrapped in an <code>FirstMapper</code>.
   */
  def swap: FirstMapper[W, B] =
    thisFirstMapper.value match {
      case First(b) => new FirstMapper(Second(b))
      case Second(w) => new FirstMapper(First(w))
    }

  /**
   * Transforms this <code>FirstMapper</code> by applying the function <code>bf</code> to the underlying <code>Else</code>'s <code>First</code> value if it is a <code>First</code>,
   * or by applying <code>wf</code> to the underlying <code>Else</code>'s <code>Second</code> value if it is a <code>Second</code>.
   *
   * @param bf the function to apply to the <code>FirstMapper</code>'s underlying <code>First</code> value, if it is a <code>First</code>
   * @param wf the function to apply to the <code>FirstMapper</code>'s underlying <code>Second</code> value, if it is a <code>Second</code>
   * @return the result of applying the appropriate one of the two passed functions, <code>bf</code> or </code>wf</code>, to the underlying <code>Else</code>'s value
   */
  def transform[C, X](bf: B => FirstMapper[C, X], wf: W => FirstMapper[C, X]): FirstMapper[C, X] =
    thisFirstMapper.value match {
      case First(b) => bf(b)
      case Second(w) => wf(w)
    }

  /**
   * Folds this <code>FirstMapper</code> into a value of type <code>V</code> by applying the given <code>bf</code> function if this is
   * a <code>First</code> else the given <code>wf</code> function if this is a <code>Second</code>.
   *
   * @param bf the function to apply to the underlying <code>Else</code>'s <code>First</code> value, if it is a <code>First</code>
   * @param wf the function to apply to the underlying <code>Else</code>'s <code>Second</code> value, if it is a <code>Second</code>
   * @return the result of applying the appropriate one of the two passed functions, <code>bf</code> or </code>wf</code>, to the underlying <code>Else</code>'s value
   */
  def fold[V](bf: B => V, wf: W => V): V =
    thisFirstMapper.value match {
      case First(b) => bf(b)
      case Second(w) => wf(w)
    }

  /**
   * A string representation for this <code>FirstMapper</code>.
   */
  override def toString = s"FirstMapper(${thisFirstMapper.value})"
}
