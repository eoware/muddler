package muddler.mudlet.items
import groovy.transform.ToString
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil
import muddler.mudlet.items.Item
import groovy.xml.XmlSlurper

@ToString
class Timer extends Item {
  String isActive
  String isFolder
  String isTempTimer = "no"
  String isOffsetTimer = "no"
  String name
  String path
  String script
  String command
  String packageName
  String hours
  String minutes
  String seconds
  String milliseconds
  String time
  List children

 Timer(Map options, Boolean read = true) {
    super(options)
    this.command = options.command ?: ""
    this.time = options.time ?: ""
    this.hours = options.hours ?: 0
    this.minutes = options.minutes ?: 0
    this.seconds = options.seconds ?: 0
    this.milliseconds = options.milliseconds ?: 0
    if (this.time == "") {
      this.time = String.format("%02d:%02d:%02d.%03d",this.hours.toInteger(),this.minutes.toInteger(), this.seconds.toInteger() , this.milliseconds.toInteger())
    }
    this.script = options.script ?: ""
    if(read) super.readScripts("timers")
  }

  def newItem(Map options) {
    return new Timer(options)
  }

   static Timer fromXml(String xml) {
        def xmlSlurper = new XmlSlurper().parseText(xml)
        Map options = [:]

        options.isActive = xmlSlurper.@isActive.text()
        options.isFolder = xmlSlurper.@isFolder.text()
        options.isTempTimer = xmlSlurper.@isTempTimer.text()
        options.isOffsetTimer = xmlSlurper.@isOffsetTimer.text()
        options.name = xmlSlurper.name.text()
        options.script = xmlSlurper.script.text()
        options.command = xmlSlurper.command.text()
        options.packageName = xmlSlurper.packageName.text()
        options.time = xmlSlurper.time.text()

        // Extracting time components (hours, minutes, seconds, milliseconds) from the time string
        if (options.time) {
            def timeParts = options.time.split(/:/)
            if (timeParts.size() > 1) {
                options.hours = timeParts[0].toInteger()
                options.minutes = timeParts[1].toInteger()
                def secondsParts = timeParts[2].split(/\./)
                options.seconds = secondsParts[0].toInteger()
                options.milliseconds = secondsParts[1].toInteger()
            }
        }

        def children = []
        xmlSlurper.children().each {
            if (it.name() == 'Timer' || it.name() == 'TimerGroup') {
                def xmlString = XmlUtil.serialize(it)
                children.add(fromXml(xmlString))
            }
        }
        options.children = children

        return new Timer(options, false)
    }

  def toXML() {
    def writer = new StringWriter()
    def xml = new MarkupBuilder(writer)
    def childString = ""
    this.children.each {
      childString = childString + it.toXML()
    }
    def header = "Timer"
    if (this.isFolder == "yes") {
      header = "TimerGroup"
    }
    xml."$header"( isActive : this.isActive, isFolder : this.isFolder, isTempTimer: this.isTempTimer, isOffsetTimer: this.isOffsetTimer) {
      name this.name
      mkp.yieldUnescaped "<script>" + this.script.trim() + "</script>"
      command this.command 
      packageName ''
      time this.time
      mkp.yieldUnescaped childString.trim()
    }
    return writer.toString()
  }
}
