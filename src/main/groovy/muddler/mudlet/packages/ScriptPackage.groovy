package muddler.mudlet.packages
import muddler.mudlet.packages.Package
import muddler.mudlet.items.Script
import groovy.xml.XmlSlurper
import groovy.xml.XmlUtil

class ScriptPackage extends Package {

  ScriptPackage(Boolean scan = true) {
    super('scripts',scan)
  }

  def toXML() {
    return super.toXML('ScriptPackage')
  }
  def newItem(Map options) {
    return new Script(options)
  }

  def newItem(Script s) {
    return s;
  }

static ScriptPackage fromXml(String xml) {
    def xmlSlurper = new XmlSlurper().parseText(xml)
    ScriptPackage scriptPackage = new ScriptPackage(false)

    xmlSlurper.children().each { node ->
        Script script = Script.fromXml( XmlUtil.serialize(node) )
        scriptPackage.children.add(script)
    }

    return scriptPackage
}


  def findFiles() {
    return super.findFiles("scripts.json")
  }
  
}