package org.scalalang.macroparadise

import scala.tools.nsc.{Global, Phase, SubComponent}
import scala.tools.nsc.plugins.{EarlyPlugin => NscEarlyPlugin, PluginComponent => NscPluginComponent}
import scala.collection.{mutable, immutable}
import org.scalalang.macroparadise.typechecker.Analyzer

class Plugin(val global: Global) extends NscEarlyPlugin {
  import global._

  val name = "macroparadise"
  val description = "Empowers production Scala compiler with latest macro developments"
  val components = List[NscPluginComponent](PluginComponent)
  override val analyzer = Some(new { val global: Plugin.this.global.type = Plugin.this.global } with Analyzer)

  object PluginComponent extends NscPluginComponent {
    val global = Plugin.this.global
    import global._

    override val runsAfter = List("parser")
    val phaseName = "macroparadise"
    override val description = "let our powers combine"

    override def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {
      override def apply(unit: CompilationUnit) {
        // do nothing: everything's already hijacked
      }
    }
  }
}
