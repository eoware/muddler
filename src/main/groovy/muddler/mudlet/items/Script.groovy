package muddler.mudlet.items
import groovy.transform.ToString
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil
import muddler.mudlet.items.Item
import groovy.xml.XmlSlurper

@ToString
class Script extends Item {
  String isActive
  String isFolder
  String packageName
  String name
  String script
  String path
  List eventHandlerList
  List children

 Script(Map options, Boolean read = true) {
    super(options)
    this.eventHandlerList = options.eventHandlerList
    this.script = options.script ?: ""
    if(read) super.readScripts("scripts")
  }

  def newItem(Map options) {
    return new Script(options)
  }
  
    static Script fromXml(String xml) {
        def xmlSlurper = new XmlSlurper().parseText(xml)
        Map options = [:]

        options.isActive = xmlSlurper.@isActive.text()
        options.isFolder = xmlSlurper.@isFolder.text()
        options.name = xmlSlurper.name.text()
        options.script = xmlSlurper.script.text()
        options.packageName = xmlSlurper.packageName.text()
        options.eventHandlerList = []

        xmlSlurper.eventHandlerList."string".each {
            options.eventHandlerList.add(it.text())
        }

        def children = []
        xmlSlurper.children().findAll { it.name() == 'Script' || it.name() == 'ScriptGroup' }.each { 
          childNode ->
            def xmlString = XmlUtil.serialize(childNode)
            children.add(fromXml(xmlString))
        }
        options.children = children

        return new Script(options, false)
    }

  def toXML() {
    def writer = new StringWriter()
    def xml = new MarkupBuilder(writer)
    def eventListString = "\n<eventHandlerList>\n"
    this.eventHandlerList.each { event ->
      eventListString = eventListString  + "<string>$event</string>\n"
    }
    eventListString = eventListString + "</eventHandlerList>"
    def childXML = ""
    this.children.each {
      childXML = childXML + it.toXML()
    }
    def header = 'Script'
    if (this.isFolder == "yes") {
      header = 'ScriptGroup'
    }
    xml."$header" ( isActive : this.isActive, isFolder : this.isFolder ) {
      name this.name
      mkp.yieldUnescaped "<script>" + this.script + "</script>"
      packageName ''
      mkp.yieldUnescaped eventListString
      mkp.yieldUnescaped childXML
    }
    return writer.toString()
  }
}
