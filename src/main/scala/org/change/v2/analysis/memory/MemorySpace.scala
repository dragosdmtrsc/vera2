package org.change.v2.analysis.memory

import org.change.v2.analysis.constraint.{EQ_E, GTE_E, GT_E, LTE_E, LT_E, _}
import org.change.v2.analysis.expression.abst.Expression
import org.change.v2.analysis.expression.concrete.SymbolicValue
import org.change.v2.analysis.expression.concrete.nonprimitive.Reference
import org.change.v2.analysis.types.{NumericType, TypeUtils}
import org.change.v2.analysis.z3.Z3Util
import org.change.v2.executor.clickabstractnetwork.ClickExecutionContext
import org.change.v2.interval.{IntervalOps, ValueSet}
import org.change.v2.util.codeabstractions._
import z3.scala.{Z3Model, Z3Solver}
import spray.json._

import scala.collection.mutable.{Map => MutableMap}

/**
*  Author: Radu Stoenescu
*  Don't be a stranger to symnet.radustoe@spamgourmet.com
*/
case class MemorySpace(val symbols: Map[String, MemoryObject] = Map.empty,
                       val rawObjects: Map[Int, MemoryObject] = Map.empty,
                       val memTags: Map[String, Int] = Map.empty,
                       val intersections : List[State] = Nil,
                       val differences : List[State] = Nil,
                       val saved : Map[String, List[State]] = Map.empty) {
  def destroyPacket(): MemorySpace = copy(rawObjects = Map.empty)

  private def resolveBy[K](id: K, m: Map[K, MemoryObject]): Option[Value] =
    m.get(id).flatMap(_.value)

  private def resolveByToObject[K](id: K, m: Map[K, MemoryObject]): Option[MemoryObject] =
    m.get(id)

  def Tag(name: String, value: Int): Option[MemorySpace] = Some(
    copy(symbols, rawObjects, memTags + (name -> value))
  )
  def UnTag(name: String): Option[MemorySpace] = Some(copy(symbols, rawObjects, memTags - name))

  /**
   * Get the currently visible value associated with a symbol.
   * @param id symol name
   * @return the value or none
   */
  def eval(id: String): Option[Value] = resolveBy(id, symbols)
  def eval(a: Int): Option[Value] = resolveBy(a, rawObjects)

  def evalToObject(id: String): Option[MemoryObject] = resolveByToObject(id, symbols)
  def evalToObject(a: Int): Option[MemoryObject] = resolveByToObject(a, rawObjects)

  def canRead(a: Int): Boolean = resolveBy(a, rawObjects).isDefined

  def isAllocated(a: Int): Boolean = rawObjects.contains(a)

  def doesNotOverlap(a: Int, size: Int): Boolean = {
    (! rawObjects.contains(a)) &&
      rawObjects.forall(kv => ! IntervalOps.intervalIntersectionIsInterval(a, a+size, kv._1, kv._1 + kv._2.size))
  }

  def canModify(a: Int, size: Int): Boolean =
    doesNotOverlap(a, size) ||
      (rawObjects.contains(a) && rawObjects(a).size == size)

  def canModifyExisting(a: Int, size: Int): Boolean = rawObjects.contains(a) && rawObjects(a).size == size

  /**
   * Checks if a given symbol is assigned to a value.
   * @param id
   * @return
   */
  def symbolIsAssigned(id: String): Boolean = { eval(id).isDefined }
  def symbolIsDefined(id: String): Boolean = { symbols.contains(id) }

  /**
   * Allocates a new empty stack for a given symbol.
   * @param id
   * @return
   */
  def Allocate(id: String): Option[MemorySpace] =
    Allocate(id, 0)

  def Allocate(id: String, size: Int): Option[MemorySpace] = {
    Some(copy(
      symbols + (id -> (if (!symbolIsDefined(id)) MemoryObject(size = size) else symbols(id).allocateNewStack)),
      rawObjects,
      memTags
    ))
  }

  def Allocate(a: Int, size: Int): Option[MemorySpace] = if (canModifyExisting(a, size))
    Some(copy(
      symbols,
      rawObjects + ( a -> rawObjects(a).allocateNewStack),
      memTags
    ))
  else if (canModify(a, size))
    Some(copy(
      symbols,
      rawObjects + ( a -> MemoryObject(size = size)),
      memTags
    ))
  else None

  /**
   * Destroys the newest stack assigned to a value.
   * @param id
   * @return
   */
  def Deallocate(id: String): Option[MemorySpace] = symbols.get(id).flatMap(_.deallocateStack).map( o =>
    copy(
      if (o.isVoid) symbols - id else symbols + (id -> o),
      rawObjects,
      memTags
    )
  )

  def Deallocate(a: Int, size: Int): Option[MemorySpace] = if (canModifyExisting(a, size))
    rawObjects.get(a).flatMap(_.deallocateStack).map(o =>
      copy(
        symbols,
        if (o.isVoid) rawObjects - a else rawObjects + (a -> o),
        memTags
      )
    )
  else
    None

  /**
   * Rewrite a symbol to a new expression.
   *
   * @param id
   * @param exp
   * @return
   */
  def Assign(id: String, exp: Expression, eType: NumericType): Option[MemorySpace] = { assignNewValue(id, exp, eType) }
  def Assign(id: String, exp: Expression): Option[MemorySpace] = Assign(id, exp, TypeUtils.canonicalForSymbol(id))

  def Assign(a: Int, exp: Expression): Option[MemorySpace] =
    if (isAllocated(a)) {
      exp match {
        case Reference(v, _) => {
          val nm = Some(copy(
            symbols,
            rawObjects + (a -> rawObjects(a).addValue(v.copy())),
            memTags
          ))
          nm
        }
        case _ => {
          val nm = Some(copy(
            symbols,
            rawObjects + (a -> rawObjects(a).addValue(Value(exp))),
            memTags
          ))
          nm
        }
      }
    }
    else
      None


  def addConstraint(id: String, c: Constraint): Option[MemorySpace] = addConstraint(id, c, false)

  def addConstraint(id: String, c: Constraint, defer: Boolean): Option[MemorySpace] = eval(id).flatMap(smb => {
    //    val (newSmb, isSolved) = checkSat(smb.copy(e = normalize(smb.e)), normalize(c))
    val (newSmb, isSolved) = (Option[Value](smb.constrain(c)), false)
    if (isSolved) {
      if (newSmb.isDefined) {
        Some(replaceValue(id, newSmb.get).get)
      }
      else {
        None
      }
    }
    else {
      val newMem = replaceValue(id, newSmb.get).get
      val subject = newMem.eval(id).get
      if (defer)
        Some(newMem)
      else {
        c match {
          case EQ_E(someE) if someE.id == subject.e.id => Some(newMem)
          case GT_E(someE) if someE.id == subject.e.id => None
          case GTE_E(someE) if someE.id == subject.e.id => Some(newMem)
          case LT_E(someE) if someE.id == subject.e.id => None
          case LTE_E(someE) if someE.id == subject.e.id => Some(newMem)
          case _ => if (!defer) memoryToOption(newMem) else Some(newMem)
        }
      }
    }
  })

  /**
    * ONLY RUN THIS after checkSat
    */
  def checkRedundant(v: Value, c: Constraint): (Value, Boolean) = {
    (v.constrain(c), false)
  }

  def addConstraint(r: Either[Int, String], c: Constraint, defer: Boolean): Option[MemorySpace] = r match {
    case Left(a) => addConstraint(a, c, defer)
    case Right(s) => addConstraint(s, c, defer)
  }


  def addConstraint(a: Int, c: Constraint): Option[MemorySpace] = addConstraint(a, c, false)

  def addConstraint(a: Int, c: Constraint, defer: Boolean): Option[MemorySpace] = eval(a).flatMap(smb => {
    val (newSmb, isSolved) = (Option[Value](smb.constrain(c)), false)
    if (isSolved) {
      if (newSmb.isDefined) {
        Some(replaceValue(a, newSmb.get).get)
      }
      else {
        None
      }
    }
    else {
      val newMem = replaceValue(a, newSmb.get).get
      val subject = newMem.eval(a).get
      if (defer)
        Some(newMem)
      else {
        c match {
          case EQ_E(someE) if someE.id == subject.e.id => Some(newMem)
          case GT_E(someE) if someE.id == subject.e.id => None
          case GTE_E(someE) if someE.id == subject.e.id => Some(newMem)
          case LT_E(someE) if someE.id == subject.e.id => None
          case LTE_E(someE) if someE.id == subject.e.id => Some(newMem)
          case _ => {
            //            println(s"Can't handle the truth for $smb added ct $c and newSmb is $newSmb")
            memoryToOption(newMem)
          }
        }
      }
    }
  })

  // FOR TESTING purposes ONLY
  //  def Constrain(id: String, c: Constraint): Option[MemorySpace] = eval(id).flatMap(smb => {
  //    val newSmb = smb.constrain(c)
  //    val newMem = replaceValue(id, newSmb).get
  //
  //    val subject = newMem.eval(id).get
  //
  //    c match {
  //      case EQ_E(someE) if someE.id == subject.e.id => Some(newMem)
  //      case GT_E(someE) if someE.id == subject.e.id => None
  //      case GTE_E(someE) if someE.id == subject.e.id => Some(newMem)
  //      case LT_E(someE) if someE.id == subject.e.id => None
  //      case LTE_E(someE) if someE.id == subject.e.id => Some(newMem)
  //      case _ => memoryToOption(newMem)
  //    }
  //  })
  //// FOR TESTING purposes ONLY
  //  def Constrain(a: Int, c: Constraint): Option[MemorySpace] = eval(a).flatMap(smb => {
  //    val newSmb = smb.constrain(c)
  //    val newMem = replaceValue(a, newSmb).get
  //
  //    val subject = newMem.eval(a).get
  //
  //    c match {
  //      case EQ_E(someE) if someE.id == subject.e.id => Some(newMem)
  //      case GT_E(someE) if someE.id == subject.e.id => None
  //      case GTE_E(someE) if someE.id == subject.e.id => Some(newMem)
  //      case LT_E(someE) if someE.id == subject.e.id => None
  //      case LTE_E(someE) if someE.id == subject.e.id => Some(newMem)
  //      case _ => memoryToOption(newMem)
  //    }
  //  })

  // FOR TESTING purposes ONLY
  private[this] def memoryToOption(m: MemorySpace): Option[MemorySpace] =
    if (m.isZ3Valid)
      Some(m)
    else
      None

  def replaceValue(id: String, v: Value): Option[MemorySpace] = symbols.get(id).flatMap(_.replaceValue(v)).map(
    mo => copy(
      symbols + (id -> mo),
      rawObjects,
      memTags
    )
  )

  def replaceValue(id: Int, v: Value): Option[MemorySpace] = rawObjects.get(id).flatMap(_.replaceValue(v)).map(
    mo => copy(
      symbols,
      rawObjects + (id -> mo),
      memTags
    )
  )

  /**
    * Pushes a new expression on the newest SSA stack of a symbol.
    *
    * @param id
    * @param exp
    * @return
    */
  def assignNewValue(id: String, exp: Expression, eType: NumericType): Option[MemorySpace] = {
    exp match {
      case Reference(v, _) => assignNewValue(id, v.copy())
      case _ => assignNewValue(id, Value(exp, eType))
    }
  }

  def assignNewValue(id: String, v: Value): Option[MemorySpace] =
    Some(copy(
      symbols + (id -> (if (symbolIsDefined(id)) symbols(id).addValue(v) else MemoryObject(size = 64).addValue(v))),
      rawObjects,
      memTags
    ))

  import org.change.v2.analysis.memory.jsonformatters.MemorySpaceToJson._
  def jsonString = this.toJson.toString

  override def toString = jsonString

  def oldToString = "Tags:" + memTags.mkString("\n") +
    "Memory values:\n" +
    symbols.map(kv => kv._1 -> ("Crt:" + kv._2.value, "Initital: " + kv._2.initialValue)).mkString("\n") +
    rawObjects.map(kv => kv._1 -> ("Crt:" + kv._2.value, "Initital: " + kv._2.initialValue)).mkString("\n") + "\n"

  def verboselyPrintObject[A](kv: (A, MemoryObject)): String = (kv._1 ->
    ("Crt:" + kv._2.value +
      "Example:" + (kv._2.value match {
      case Some(v) => exampleFor(v).toString
      case _ => "-"
    }) +
      "Initital: " + kv._2.initialValue +
      "Example:" + (kv._2.initialValue match {
      case Some(v) => exampleFor(v).toString
      case _ => "-"
    }))).toString()

  def verboseToString = "Tags:" + memTags.mkString("\n") +
    "Memory values:\n" +
    symbols.map(verboselyPrintObject(_)).mkString("\n") +
    rawObjects.map(verboselyPrintObject(_)).mkString("\n") + "\n"

  def valid: Boolean = isZ3Valid

  def buildSolver: Z3Solver =
    if (false)
      solverCache
    else {
      solverCache = (symbols.values ++ rawObjects.values).foldLeft(Z3Util.solver) { (slv, mo) =>
        mo.value match {
          case Some(v) => v.toZ3(Some(slv))._2.get
          case _ => slv
        }
      }
      isZ3SolverCacheValid = true
      //    System.out.println(solverCache)
      solverCache
    }


  private var isZ3SolverCacheValid = false
  private var solverCache: Z3Solver = _
  private var isZ3ModelCacheValid = false
  private var modelCache: Option[Z3Model] = _

  def isZ3Valid: Boolean = {
    //    val job = MemorySpace.service.submit(new Callable[Boolean]() {
    //      override def call(): Boolean = {
    //        val crt = System.currentTimeMillis()
    val aux = buildSolver.check().get
    //        MemorySpace.incZ3Time(System.currentTimeMillis()-crt)
    //        MemorySpace.incZ3Call
    aux
    //      }
    //    })
    //
    //    job.get()
  }

  def buildModel: Option[Z3Model] = if (isZ3ModelCacheValid)
    modelCache
  else {
    modelCache = if (buildSolver.check().get) Some(buildSolver.getModel()) else None
    isZ3ModelCacheValid = true
    modelCache
  }

  def exampleFor(id: String): Option[Int] = resolveBy(id, symbols).flatMap(exampleFor(_))

  def exampleFor(a: Int): Option[Int] = resolveBy(a, rawObjects).flatMap(exampleFor(_))

  def exampleFor(v: Value): Option[Int] = buildModel.flatMap(m => {
    val e = m.evalAs[Int](v.e.toZ3(Some(buildSolver))._1)
    e
  })

  def concretizeSymbols = (rawObjects.map(kv => kv._1.toString -> kv._2)).map { kv =>
    (kv._1 -> kv._2.value.flatMap(exampleFor(_)))
  }
}

object MemorySpace {
  /**
   * Empty memory.
   * @return
   */
   var z3Time = 0L;
   var z3Call = 0L;

   def incZ3Time(i:Long) = z3Time+=i
   def incZ3Call() = z3Call+=1
   
  def clean: MemorySpace = new MemorySpace()

  /**
   * ATTENTION: Remove the ugly get and make a hl func for this
   * @param symbols What symbols should the memory contain initially.
   * @return
   */
  def cleanWithSymolics(symbols: List[String]) = symbols.foldLeft(clean)((mem, s) => mem.Assign(s, SymbolicValue()).get)

//  def service = ClickExecutionContext.getService
}
