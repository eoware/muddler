package muddler.mudlet.items
import groovy.transform.ToString
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil
import muddler.mudlet.items.Item
import groovy.xml.XmlSlurper
import muddler.Echo

@ToString
class Trigger extends Item {
  String isActive
  String isFolder
  String isTempTrigger
  String isMultiline
  String isPerlSlashGOption
  String isColorizerTrigger
  String isFilterTrigger
  String isSoundTrigger
  String isColorTrigger
  String isColorTriggerFg
  String isColorTriggerBg
  String name
  String path
  String script
  String triggerType
  String conditonLineDelta
  String mStayOpen
  String mCommand
  String mFgColor
  String mBgColor
  String mSoundFile
  String colorTriggerBgColor
  String colorTriggerFgColor
  String packageName
  List regexCodeList
  List regexCodePropertyList
  List patterns
  List children

 Trigger(Map options, Boolean read = true) {
    super(options)

//    (new Echo()).echo("New trigger ----")
    this.script = options.script ?: ""
    if(read) super.readScripts("triggers")
    this.mCommand = options.command ?: ""
    this.triggerType = 0
    this.isMultiline = super.truthiness(options.multiline)
    this.conditonLineDelta = "0"

    //(new Echo()).echo("New trigger ------")

    if (this.isMultiline == "yes") {
      this.conditonLineDelta = options.multilineDelta ?: this.conditonLineDelta
    }
    this.isPerlSlashGOption = super.truthiness(options.matchall)
    this.isFilterTrigger = super.truthiness(options.filter)
    this.mStayOpen = options.fireLength ?: "0"
    this.mSoundFile = ""
    this.isSoundTrigger = "no"

  
    //(new Echo()).echo("New trigger --------")

    if (options.soundFile) {
      this.mSoundFile = options.soundFile
      this.isSoundTrigger = "yes"
    }
    this.isColorizerTrigger = super.truthiness(options.highlight)
    this.mFgColor = "#ff0000"
    this.mBgColor = "#ffff00"
    if (this.isColorizerTrigger == "yes") {
      this.mFgColor = options.highlightFG ?: this.mFgColor
      this.mBgColor = options.highlightBG ?: this.mBgColor
    }


    //(new Echo()).echo("New trigger --------")


    this.colorTriggerBgColor = "#000000"
    this.colorTriggerFgColor = "#000000"
    this.patterns = options.patterns ?: []
    this.regexCodeList = []
    this.regexCodePropertyList = []

    //(new Echo()).echo("New trigger ------")

    this.patterns.each { pattern ->

      //(new Echo()).echo("New trigger ------ pattern = " + pattern)

      def patternTypeNumber = patternTypeToNumber(pattern.type) // this sets a default if it wasn't given
      if (patternTypeNumber == '6') { // 6 is the number for color trigger. 
        this.isColorTrigger = "yes"
        def colorArray = pattern.pattern.split(",")
        def fg = colorArray[0]
        def bg = colorArray[1]
        this.isColorTriggerBg = "yes"
        this.isColorTriggerFg = "yes"
        if (fg == "IGNORE") { this.isColorTriggerFg = "no" }
        if (bg == "IGNORE") { this.isColorTriggerBg = "no" }
        this.regexCodeList.add("<string>ANSI_COLORS_F{$fg}_B{$bg}</string>")
        this.regexCodePropertyList.add("<integer>$patternTypeNumber</integer>")
      } else if (patternTypeNumber == '7') { // prompt triggers have empty string for regexCode
        this.regexCodeList.add("<string></string>")
        this.regexCodePropertyList.add("<integer>$patternTypeNumber</integer>")
      } else {
        this.regexCodeList.add("<string>${XmlUtil.escapeXml(pattern.pattern)}</string>")
        this.regexCodePropertyList.add("<integer>$patternTypeNumber</integer>")
      }
    }

     // (new Echo()).echo("New trigger ----")
  }

   static Trigger fromXml(String xml) {

        def e = new Echo()
        //e.echo("FromXml -> slurping")

        def xmlSlurper = new XmlSlurper().parseText(xml)
        Map options = [:]

        options.isActive = xmlSlurper.@isActive.text()
        options.isFolder = xmlSlurper.@isFolder.text()
        options.isTempTrigger = xmlSlurper.@isTempTrigger.text()
        options.isMultiline = xmlSlurper.@isMultiline.text()
        options.isPerlSlashGOption = xmlSlurper.@isPerlSlashGOption.text()
        options.isColorizerTrigger = xmlSlurper.@isColorizerTrigger.text()
        options.isFilterTrigger = xmlSlurper.@isFilterTrigger.text()
        options.isColorTrigger = xmlSlurper.@isColorTrigger.text()
        options.isColorTriggerFg = xmlSlurper.@isColorTriggerFg.text()
        options.isColorTriggerBg = xmlSlurper.@isColorTriggerBg.text()
        options.name = xmlSlurper.name.text().replaceAll("/", "-")

        //e.echo("FromXml -> slurping: " + options.name)

        options.script = xmlSlurper.script.text()
        options.triggerType = xmlSlurper.triggerType.text()
        options.conditonLineDelta = xmlSlurper.conditonLineDelta.text()
        options.mStayOpen = xmlSlurper.mStayOpen.text()
        options.mCommand = xmlSlurper.mCommand.text()
        options.mFgColor = xmlSlurper.mFgColor.text()
        options.mBgColor = xmlSlurper.mBgColor.text()
        options.mSoundFile = xmlSlurper.mSoundFile.text()
        options.colorTriggerBgColor = xmlSlurper.colorTriggerBgColor.text()
        options.colorTriggerFgColor = xmlSlurper.colorTriggerFgColor.text()
        options.packageName = xmlSlurper.packageName.text()
        options.path = xmlSlurper.path.text()

        // Patterns and regex codes are expected to be in specific child elements
        options.patterns = []

        //e.echo("FromXml -> slurping regexCodeList")

        def regexCodeList = xmlSlurper.regexCodeList."string"
        def regexCodePropertyList = xmlSlurper.regexCodePropertyList."integer"



        for (int i = 0; i < regexCodeList.size(); i++) {
            def pattern = regexCodeList[i].text()
            //e.echo("From xml zzz: rettype = " + regexCodePropertyList[i].text().getClass().getSimpleName())
            //e.echo( "From xml zzz: num = " + regexCodePropertyList[i].text())
            def type = numberToPatternType("" + regexCodePropertyList[i].text())
            //e.echo( "From xml zzz: type = " + type )
            def entry = [pattern: pattern, type: type]
            options.patterns.add(entry)
        }
        
        //e.echo("FromXml -> slurping children")

        def children = []
        xmlSlurper.children().each {
            if (it.name() == 'Trigger' || it.name() == 'TriggerGroup') {
                def xmlString = XmlUtil.serialize(it)
                children.add(fromXml(xmlString))
            }
        }
        options.children = children

        //e.echo("FromXml -> done slurping")

        return new Trigger(options, false)
    }

  def newItem(Map options) {
    return new Trigger(options)
  }

  def toXML() {
    def writer = new StringWriter()
    def xml = new MarkupBuilder(writer)
    def childString = ""
    this.children.each {
      childString = childString + it.toXML()
    }
    def regexCodeString = "<regexCodeList>\n" + this.regexCodeList.join("\n") + "</regexCodeList>"
    def regexCodePropertyListString = "<regexCodePropertyList>\n" + this.regexCodePropertyList.join("\n") + "</regexCodePropertyList>"
    def header = "Trigger"
    if (this.isFolder == "yes") {
      header = "TriggerGroup"
    }
    xml."$header" (isActive: this.isActive, isFolder: this.isFolder, isMultiline: this.isMultiline, isPerlSlashGOption: this.isPerlSlashGOption, isColorizerTrigger: this.isColorizerTrigger, isFilterTrigger: this.isFilterTrigger, isColorTrigger: this.isColorTrigger, isColorTriggerFg: this.isColorTriggerFg, isColorTriggerBg: this.isColorTriggerBg) {
      name this.name
      mkp.yieldUnescaped "<script>${this.script}</script>"
      triggerType this.triggerType
      conditonLineDelta this.conditonLineDelta
      mStayOpen this.mStayOpen
      mCommand this.mCommand
      packageName ''
      path this.path
      mFgColor this.mFgColor
      mBgColor this.mBgColor
      mSoundFile this.mSoundFile
      colorTriggerFgColor this.colorTriggerFgColor
      colorTriggerBgColor this.colorTriggerBgColor
      mkp.yieldUnescaped regexCodeString
      mkp.yieldUnescaped regexCodePropertyListString
      mkp.yieldUnescaped childString
    }
    return writer.toString()
  }

  static def patternTypeToNumber(String patternType) {
    def typeMap = [
      substring: '0',
      regex: '1',
      startOfLine: '2',
      exactMatch: '3',
      lua: '4',
      spacer: '5',
      color: '6',
      colour: '6',
      prompt: '7'
    ]
    return typeMap[patternType] ?: '0'
  }

  static def numberToPatternType(String num) {
    def typeMap = [
      '0': 'substring',
      '1': 'regex',
      '2': 'startOfLine',
      '3': 'exactMatch',
      '4': 'lua',
      '5': 'spacer',
      '6': 'color',
      '7': 'colour',
      '8': 'prompt'
    ]
    return typeMap[num] ?: 'regex'
  }
}