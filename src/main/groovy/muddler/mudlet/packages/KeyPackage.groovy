package muddler.mudlet.packages
import muddler.mudlet.packages.Package
import muddler.mudlet.items.Key
import groovy.xml.XmlSlurper
import groovy.xml.XmlUtil

class KeyPackage extends Package {

  KeyPackage(Boolean scan = true) {
    super('keys', scan)
  }

  def toXML() {
    return super.toXML('KeyPackage')
  }


static KeyPackage fromXml(String xml) {
    def xmlSlurper = new XmlSlurper().parseText(xml)
    KeyPackage keyPackage = new KeyPackage(false)

    xmlSlurper.children().each { node ->
        Key key = Key.fromXml( XmlUtil.serialize(node) )
        keyPackage.children.add(key)
    }

    return keyPackage
}


  def newItem(Map options) {
    return new Key(options)
  }

  def newItem(Key k) {
    return k
  }

  def findFiles() {
    return super.findFiles("keys.json")
  }
  
}