package muddler.mudlet.packages
import muddler.mudlet.packages.Package
import muddler.mudlet.items.Alias
import groovy.xml.XmlSlurper
import groovy.xml.XmlUtil

class AliasPackage extends Package {

  AliasPackage(Boolean scan = true) {
    super('aliases',scan)
  }

  def toXML() {
    return super.toXML('AliasPackage')
  }
  def newItem(Map options) {
    return new Alias(options)
  }
  def newItem(Alias a) {
    return a
  }

  def findFiles() {
    return super.findFiles("aliases.json")
  }

    static AliasPackage fromXml(String xml) {
        def xmlSlurper = new XmlSlurper().parseText(xml)
        AliasPackage aliasPackage = new AliasPackage(false)

        // Assuming the structure of the XML matches that of Alias items directly under the AliasPackage node
        xmlSlurper.children().each { node ->
            Alias alias = Alias.fromXml( XmlUtil.serialize(node) )
            aliasPackage.children.add(alias)
        }

        return aliasPackage
    }

}