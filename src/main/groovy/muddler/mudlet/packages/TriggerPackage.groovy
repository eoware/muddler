package muddler.mudlet.packages
import muddler.mudlet.packages.Package
import muddler.mudlet.items.Trigger
import groovy.xml.XmlSlurper
import muddler.Echo
import groovy.xml.XmlUtil

class TriggerPackage extends Package {

  TriggerPackage(Boolean scan = true) {
    super('triggers', scan)
  }

  def toXML() {
    return super.toXML('TriggerPackage')
  }

  static TriggerPackage fromXml(String xml) {
      def xmlSlurper = new XmlSlurper().parseText(xml)
      TriggerPackage triggerPackage = new TriggerPackage(false)

      xmlSlurper.children().each { node ->

//          (new Echo()).echo("trigger node= " + node.name)
          Trigger trigger = Trigger.fromXml( XmlUtil.serialize(node) )
          triggerPackage.children.add(trigger)
//         (new Echo()).echo("trigger node= " + node.name + " done")
      }



      return triggerPackage
  }


  def newItem(Map options) {
    return new Trigger(options)
  }

  def newItem(Trigger t) {
    return t
  }

  def findFiles() {
    return super.findFiles("triggers.json")
  }
  
}