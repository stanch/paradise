import scala.reflect.macros.Context
import scala.language.experimental.macros
import scala.annotation.StaticAnnotation

object prettyMacro {
  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    import Flag._
    val result = {
      annottees.map(_.tree).toList match {
        case ModuleDef(mods, name, Template(parents, self, body)) :: Nil =>
          val toStringMethod = DefDef(Modifiers(OVERRIDE), newTermName("toString"), List(), List(List()), TypeTree(), Literal(Constant(name.toString)))
          ModuleDef(mods, name, Template(parents, self, body :+ toStringMethod))
      }
    }
    c.Expr[Any](result)
  }
}

class pretty extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro prettyMacro.impl
}

package pkg {
  class pretty extends StaticAnnotation {
    def macroTransform(annottees: Any*) = macro prettyMacro.impl
  }
}
