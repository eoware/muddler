package muddler.mudlet.packages
import muddler.mudlet.packages.Package
import muddler.mudlet.items.Timer
import groovy.xml.XmlSlurper
import groovy.xml.XmlUtil

class TimerPackage extends Package {

  TimerPackage(Boolean scan = true) {
      super('timers', scan)
  }

  def toXML() {
    return super.toXML('TimerPackage')
  }
  def newItem(Map options) {
    return new Timer(options)
  }

  def findFiles() {
    return super.findFiles("timers.json")
  }

  static TimerPackage fromXml(String xml) {
    def xmlSlurper = new XmlSlurper().parseText(xml)
    TimerPackage timerPackage = new TimerPackage(false)

    xmlSlurper.children().each { node ->
        Timer timer = Timer.fromXml( XmlUtil.serialize(node) )
        timerPackage.children.add(timer)
    }

    return timerPackage
}

  
}