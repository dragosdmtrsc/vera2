package org.change.v2.analysis.processingmodels

import org.change.v2.analysis.memory.{State, MemorySpace}

/**
 * Author: Radu Stoenescu
 * Don't be a stranger,  symnetic.7.radustoe@spamgourmet.com
 */
package object instructions {

  private val isDebugging = false;


  def optionToStatePair(previousState: State, error: ErrorCause,
                        forceFail: Boolean = false)(block: State => Option[MemorySpace]): (List[State], List[State]) = {
    val nm = block(previousState);
    nm.map(m => (
      List(previousState.copy(memory = m, errorCause = None)),
      Nil)).
      getOrElse(
        (Nil,
          // DD: Consider these flags
          if (isDebugging || forceFail)
            List(
              previousState.copy(errorCause = Some(error))
            )
          else {
            Nil
          }
        )
      )
  };

  def stateToError(previousState: State, error: ErrorCause) = optionToStatePair(previousState, error, true)(_ => None)
}
