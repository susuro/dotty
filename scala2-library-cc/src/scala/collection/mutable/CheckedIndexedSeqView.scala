/*
 * Scala (https://www.scala-lang.org)
 *
 * Copyright EPFL and Lightbend, Inc.
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

package scala
package collection
package mutable
import language.experimental.captureChecking
import caps.cap

private[mutable] trait CheckedIndexedSeqView[+A] extends IndexedSeqView[A] {

  protected val mutationCount: () ->{cap.rd} Int

  override def iterator: Iterator[A]^{this} = new CheckedIndexedSeqView.CheckedIterator(this, mutationCount())
  override def reverseIterator: Iterator[A]^{this} = new CheckedIndexedSeqView.CheckedReverseIterator(this, mutationCount())

  override def appended[B >: A](elem: B): IndexedSeqView[B]^{this} = new CheckedIndexedSeqView.Appended(this, elem)(mutationCount)
  override def prepended[B >: A](elem: B): IndexedSeqView[B]^{this} = new CheckedIndexedSeqView.Prepended(elem, this)(mutationCount)
  override def take(n: Int): IndexedSeqView[A]^{this} = new CheckedIndexedSeqView.Take(this, n)(mutationCount)
  override def takeRight(n: Int): IndexedSeqView[A]^{this} = new CheckedIndexedSeqView.TakeRight(this, n)(mutationCount)
  override def drop(n: Int): IndexedSeqView[A]^{this} = new CheckedIndexedSeqView.Drop(this, n)(mutationCount)
  override def dropRight(n: Int): IndexedSeqView[A]^{this} = new CheckedIndexedSeqView.DropRight(this, n)(mutationCount)
  override def map[B](f: A => B): IndexedSeqView[B]^{this, f} = new CheckedIndexedSeqView.Map(this, f)(mutationCount)
  override def reverse: IndexedSeqView[A]^{this} = new CheckedIndexedSeqView.Reverse(this)(mutationCount)
  override def slice(from: Int, until: Int): IndexedSeqView[A]^{this} = new CheckedIndexedSeqView.Slice(this, from, until)(mutationCount)
  override def tapEach[U](f: A => U): IndexedSeqView[A]^{this, f} = new CheckedIndexedSeqView.Map(this, { (a: A) => f(a); a})(mutationCount)

  override def concat[B >: A](suffix: IndexedSeqView.SomeIndexedSeqOps[B]): IndexedSeqView[B]^{this} = new CheckedIndexedSeqView.Concat(this, suffix)(mutationCount)
  override def appendedAll[B >: A](suffix: IndexedSeqView.SomeIndexedSeqOps[B]): IndexedSeqView[B]^{this} = new CheckedIndexedSeqView.Concat(this, suffix)(mutationCount)
  override def prependedAll[B >: A](prefix: IndexedSeqView.SomeIndexedSeqOps[B]): IndexedSeqView[B]^{this} = new CheckedIndexedSeqView.Concat(prefix, this)(mutationCount)
}

private[mutable] object CheckedIndexedSeqView {
  import IndexedSeqView.SomeIndexedSeqOps

  @SerialVersionUID(3L)
  private[mutable] class CheckedIterator[A](self: IndexedSeqView[A]^, mutationCount: ->{cap.rd} Int)
    extends IndexedSeqView.IndexedSeqViewIterator[A](self) {
    private[this] val expectedCount = mutationCount
    override def hasNext: Boolean = {
      MutationTracker.checkMutationsForIteration(expectedCount, mutationCount)
      super.hasNext
    }
  }

  @SerialVersionUID(3L)
  private[mutable] class CheckedReverseIterator[A](self: IndexedSeqView[A]^, mutationCount: ->{cap.rd} Int)
    extends IndexedSeqView.IndexedSeqViewReverseIterator[A](self) {
    private[this] val expectedCount = mutationCount
    override def hasNext: Boolean = {
      MutationTracker.checkMutationsForIteration(expectedCount, mutationCount)
      super.hasNext
    }
  }

  @SerialVersionUID(3L)
  class Id[+A](underlying: SomeIndexedSeqOps[A]^)(protected val mutationCount: () ->{cap.rd} Int)
    extends IndexedSeqView.Id(underlying) with CheckedIndexedSeqView[A]

  @SerialVersionUID(3L)
  class Appended[+A](underlying: SomeIndexedSeqOps[A]^, elem: A)(protected val mutationCount: () ->{cap.rd} Int)
    extends IndexedSeqView.Appended(underlying, elem) with CheckedIndexedSeqView[A]

  @SerialVersionUID(3L)
  class Prepended[+A](elem: A, underlying: SomeIndexedSeqOps[A]^)(protected val mutationCount: () ->{cap.rd} Int)
    extends IndexedSeqView.Prepended(elem, underlying) with CheckedIndexedSeqView[A]

  @SerialVersionUID(3L)
  class Concat[A](prefix: SomeIndexedSeqOps[A]^, suffix: SomeIndexedSeqOps[A]^)(protected val mutationCount: () ->{cap.rd} Int)
    extends IndexedSeqView.Concat[A](prefix, suffix) with CheckedIndexedSeqView[A]

  @SerialVersionUID(3L)
  class Take[A](underlying: SomeIndexedSeqOps[A]^, n: Int)(protected val mutationCount: () ->{cap.rd} Int)
    extends IndexedSeqView.Take(underlying, n) with CheckedIndexedSeqView[A]

  @SerialVersionUID(3L)
  class TakeRight[A](underlying: SomeIndexedSeqOps[A]^, n: Int)(protected val mutationCount: () ->{cap.rd} Int)
    extends IndexedSeqView.TakeRight(underlying, n) with CheckedIndexedSeqView[A]

  @SerialVersionUID(3L)
  class Drop[A](underlying: SomeIndexedSeqOps[A]^, n: Int)(protected val mutationCount: () ->{cap.rd} Int)
    extends IndexedSeqView.Drop[A](underlying, n) with CheckedIndexedSeqView[A]

  @SerialVersionUID(3L)
  class DropRight[A](underlying: SomeIndexedSeqOps[A]^, n: Int)(protected val mutationCount: () ->{cap.rd} Int)
    extends IndexedSeqView.DropRight[A](underlying, n) with CheckedIndexedSeqView[A]

  @SerialVersionUID(3L)
  class Map[A, B](underlying: SomeIndexedSeqOps[A]^, f: A => B)(protected val mutationCount: () ->{cap.rd} Int)
    extends IndexedSeqView.Map(underlying, f) with CheckedIndexedSeqView[B]

  @SerialVersionUID(3L)
  class Reverse[A](underlying: SomeIndexedSeqOps[A]^)(protected val mutationCount: () ->{cap.rd} Int)
    extends IndexedSeqView.Reverse[A](underlying) with CheckedIndexedSeqView[A] {
    override def reverse: IndexedSeqView[A] = underlying match {
      case x: IndexedSeqView[A] => x
      case _ => super.reverse
    }
  }

  @SerialVersionUID(3L)
  class Slice[A](underlying: SomeIndexedSeqOps[A]^, from: Int, until: Int)(protected val mutationCount: () ->{cap.rd} Int)
    extends AbstractIndexedSeqView[A] with CheckedIndexedSeqView[A] {
    protected val lo = from max 0
    protected val hi = (until max 0) min underlying.length
    protected val len = (hi - lo) max 0
    @throws[IndexOutOfBoundsException]
    def apply(i: Int): A = underlying(lo + i)
    def length: Int = len
  }
}
