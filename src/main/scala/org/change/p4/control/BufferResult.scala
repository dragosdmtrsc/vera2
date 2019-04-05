package org.change.p4.control

// T is a region, the 4 outcomes need not be
// disjoint
case class BufferResult[T](cloned: T, goesOn: T, recirculated: T, dropped: T)
