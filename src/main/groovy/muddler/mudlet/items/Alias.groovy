package muddler.mudlet.items
import groovy.transform.ToString
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil
import muddler.mudlet.items.Item
import groovy.xml.XmlSlurper
import muddler.Echo

@ToString
class Alias extends Item {
  String isActive
  String isFolder
  String name
  String script
  String packageName
  String command
  String regex
  String path
  List children
  
 Alias(Map options, Boolean read = true) {
    super(options)
    this.command = options.command ?: ""
    this.regex = options.regex ?: ""
    this.script = options.script ?: ""
    if(read) super.readScripts("aliases")
  }
 
  def newItem(Alias a) {
    return a
  }

  def newItem(Map options) {
    return new Alias(options)
  }
  
  def toXML() {
    def writer = new StringWriter()
    def xml = new MarkupBuilder(writer)
    def childString = ""
    this.children.each {
      childString = childString + it.toXML()
    }
    def header = 'Alias'
    if (this.isFolder == "yes") {
      header = 'AliasGroup'
    }
    xml."$header" ( isActive : this.isActive, isFolder : this.isFolder ) {
      name this.name
      mkp.yieldUnescaped "<script>" + this.script + "</script>"
      command this.command 
      packageName ''
      regex this.regex
      mkp.yieldUnescaped childString
    }
    return writer.toString()
  }
  
    static Alias fromXml(String xml) {
        def xmlSlurper = new XmlSlurper().parseText(xml)
        Map options = [:]

        // Extract attributes
        options.isActive = xmlSlurper.@isActive
        options.isFolder = xmlSlurper.@isFolder
        options.name = xmlSlurper.name.text()
        options.script = xmlSlurper.script.text()
        options.command = xmlSlurper.command.text()
        options.regex = xmlSlurper.regex.text()
        options.packageName = xmlSlurper.packageName.size() > 0 ? xmlSlurper.packageName.text() : ''
        
        // Handle children if present
        List children = []
        xmlSlurper.children().each {
            if (it.name() == 'Alias' || it.name() == 'AliasGroup') {
              def xmlString = XmlUtil.serialize(it)
              children.add(fromXml(xmlString))
            }
        }
        options.children = children

        // Create new Alias instance with options
        return new Alias(options,false)
    }

}