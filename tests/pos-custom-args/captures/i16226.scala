@annotation.capability class Cap

class LazyRef[T](val elem: () => T):
  val get: () ->{elem} T = elem
  def map[U](f: T => U): LazyRef[U]^{f, this} =
    new LazyRef(() => f(elem()))

def map[A, B](ref: LazyRef[A]^, f: A => B): LazyRef[B]^{f, ref} =
  new LazyRef(() => f(ref.elem()))

def main(io: Cap) = {
  def mapd[A, B]: (LazyRef[A]^{io}, A => B) => LazyRef[B]^ =
    (ref1, f1) => map[A, B](ref1, f1)
}
