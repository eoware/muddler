package muddler
import groovy.json.JsonSlurper
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil
import groovy.cli.picocli.CliBuilder
import static groovy.io.FileType.*
import muddler.mudlet.packages.*
import java.util.regex.Pattern
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import groovy.ant.AntBuilder
import muddler.Echo
import muddler.Version
import muddler.Generator

class App {
  static void main(String[] args) {
    def e = new Echo()
    def cli = new CliBuilder(usage: 'muddler [-v|--version] [-h|--help] [-p|--parse <filename>]')
    cli.with {
      h longOpt: 'help', 'Show usage information'
      v longOpt: 'version', 'Print version information and exit'
      g longOpt: 'generate', 'Create a new muddler project by answering some questions.'
      d longOpt: 'default', 'Create a new default muddler template project named MyProject in the MyProject directory'
      p longOpt: 'parse', args: 1, argName: 'filename', 'Parse a package XML file into the source directories.'
    }
    def options = cli.parse(args)
    if (!options) {
      return
    }
    // Handle each option
    if (options.h) {
      cli.usage()
      return
    }
    if (options.v) {
      println "muddler version: ${Version.version}"
      return
    }
    if (options.g) {
      println "Creating new muddler project!"
      new Generator().create(false)
      return
    }
    if (options.d) {
      e.echo "Creating a new muddler project with all src dirs and default options."
      new Generator().create(true)
      return
    }
    if (options.p) {
      String filename = options.p
      println "Parsing package XML file: $filename"
      parse(filename)
      return
    }

    def srcDir = new File('./src')
    if (!srcDir.exists()) {
      println "muddler requires a src directory to read your package contents from, and cannot find it. Please see https://github.com/demonnic/muddler#usage for more information on the file layout for muddler."
      return
    }
    e.echo "Beginning build. Using muddler version ${Version.version}"
    def mfile = new File('./mfile')
    def readme = new File('./README.md')
    def packageName = ""
    def packageVersion = ""
    def packageAuthor = ""
    def packageTitle = ""
    def packageDesc = ""
    def packageIcon = ""
    def packageDeps = ""
    def outputFile = false
    def now = ZonedDateTime.now()
    def dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")
    def packageTimestamp = dtf.format(now)
    if (readme.exists()) {
      e.echo("Reading README.md file for package description")
      packageDesc = readme.text
    }
    if (mfile.exists()) {
      e.echo("Pulling metadata from mfile")
      def config
      try {
        config = new JsonSlurper().parse(mfile)
      } catch (groovy.json.JsonException ex) {
        e.error("Problem reading mfile, details follow:", ex)
      }
      packageName = config.package ?: packageName
      e.echo("Name        : $packageName")
      packageVersion = config.version ?: packageVersion
      e.echo("Version     : $packageVersion")
      packageAuthor = config.author ?: packageAuthor
      e.echo("Author      : $packageAuthor")
      packageTitle = config.title ?: packageTitle
      e.echo("Title       : $packageTitle")
      packageDesc = config.description ?: packageDesc
      e.echo("Description : $packageDesc")
      packageIcon = config.icon ?: packageIcon
      e.echo("Icon file   : $packageIcon")
      packageDeps = config.dependencies ?: packageDeps
      e.echo("Dependencies: $packageDeps")
      outputFile = config.outputFile ?: outputFile
    }
    if (packageName == "") {
      e.echo("Package name not set via mfile, using directory name")
      def fullPath = System.properties['user.dir']
      packageName = fullPath.split(Pattern.quote(File.separator))[-1]
      e.echo("Name       : $packageName")
    }

    if (outputFile) {
      e.echo("Will write .output file at root of project with json object containing package name and file location at build end")
    }

    e.echo("Spinning up new AntBuilder to do our dirty work but making it silent")
    def ant = new AntBuilder()
    ant.project.buildListeners[0].messageOutputLevel=1
    def outputDir = new File('build')
    e.echo("Cleaning build directory")
    outputDir.deleteDir()
    def tmp = new File(outputDir, 'tmp')
    tmp.mkdirs()
    // filter our source files from src into build/filtered/src and replace @PKGNAME@ with the package name as used by Mudlet
    // no more images failing to load because the package name changed or you bumped version
    e.echo("Filtering @PKGNAME@ and __PKGNAME__ to '$packageName' and @VERSION@ and __VERSION__ to '$packageVersion'")
    ant.copy(todir:'build/filtered/src') {
      fileset(dir: "src/") {
        exclude(name: "resources/")
      }
      filterset(begintoken: "__", endtoken: "__") {
        filter(token: "PKGNAME", value: packageName)
        filter(token: "VERSION", value: packageVersion)
      }
      filterset(){
        filter(token: "PKGNAME", value: packageName)
        filter(token: "VERSION", value: packageVersion)
      }
    }
    
    // now create the individual item type packages
    def aliasP = new AliasPackage()
    def scriptP = new ScriptPackage()
    def timerP = new TimerPackage()
    def triggerP = new TriggerPackage()
    def keyP = new KeyPackage()
    def builder = new StreamingMarkupBuilder()
    builder.encoding = 'UTF-8'
    e.echo("Converting scanned data to Mudlet package XML now")
    def mudletPackage = builder.bind {
      mkp.xmlDeclaration()
      mkp.yieldUnescaped '<!DOCTYPE MudletPackage>'
      'MudletPackage'(version: "1.001") {

        mkp.yieldUnescaped triggerP.toXML()
        mkp.yieldUnescaped timerP.toXML()
        mkp.yieldUnescaped aliasP.toXML()
        mkp.yieldUnescaped scriptP.toXML()
        mkp.yieldUnescaped keyP.toXML()
      }
    }
    def mpXML = XmlUtil.serialize(mudletPackage)
    
        // Placeholder map
    def placeholders = [:]
    int counter = 0

    // Step 1: Extract and replace <script> content
    def modifiedXmlString = mpXML.replaceAll(/(?s)<script>(.*?)<\/script>/) { match ->
        String placeholderCounter = String.format("%010d", counter++)
        String key = "PLACEHOLDER_${placeholderCounter}"
        placeholders[key] = match[1] // Save the content excluding <script> tags
        return "<script>${key}</script>" // Replace with placeholder
    }

    // Step 2: Remove extra blank lines (not inside script tags now)
    modifiedXmlString = modifiedXmlString.replaceAll("(?m)^\s*\n", "")

    // Step 3: Restore <script> content
    placeholders.each { key, value ->
        modifiedXmlString = modifiedXmlString.replace(key, value)
    }
    
    mpXML = modifiedXmlString
    e.echo("XML created successfully, writing it to disk")
    try {
      new File(outputDir, packageName + ".xml").withWriter('UTF-8') { writer ->
        writer.write(mpXML)
        writer.flush()
      }
    } catch (Exception ex) {
      e.error("Could not write the XML file because:", ex)
    }

    e.echo("Creating config.lua file contents")
    def configLua = "mpackage = [[$packageName]]\n"
    if (! packageAuthor.isEmpty()) {
      configLua += "author = [[$packageAuthor]]\n"
    }
    def validIcon = false
    if (! packageIcon.isEmpty()) {
      def iconFile = new File("src${File.separator}resources${File.separator}$packageIcon")
      if (iconFile.exists()) {
        configLua += "icon = [[$packageIcon]]\n"
        validIcon = true
      }
    }
    if (! packageTitle.isEmpty()) {
      configLua += "title = [[$packageTitle]]\n"
    }
    if (! packageDesc.isEmpty()) {
      configLua += "description = [[$packageDesc]]\n"
    }
    if (! packageVersion.isEmpty()) {
      configLua += "version = [[$packageVersion]]\n"
    }
    if (! packageDeps.isEmpty()) {
      configLua += "dependencies = [[$packageDeps]]\n"
    }
    configLua += "created = [[$packageTimestamp]]\n"
    e.echo("config.lua contents created, writing to disk")
    try {
      new File(tmp, 'config.lua').withWriter { writer ->
        writer.write(configLua)
        writer.flush()
      }
    } catch (Exception ex) {
      e.error("Error writing config.lua file:", ex)
    }

    def resDir = new File("src${File.separator}resources")
    if (resDir.exists()) {
      e.echo("Copying files from src/resources to package root")
      ant.copy(toDir: 'build/tmp') {
        fileset(dir: 'src/resources')
      }
    }
    if (validIcon) {
      e.echo("Copying icon file into place from src${File.separator}resources${File.separator}$packageIcon to .mudlet${File.separator}Icon${File.separator}$packageIcon")
      def iconDir = new File(tmp, '.mudlet/Icon')
      iconDir.mkdirs()
      ant.copy(file: "build/tmp/$packageIcon", tofile: "build/tmp/.mudlet/Icon/$packageIcon")
    }

    ant.copy(toDir: 'build/tmp') {
      fileset(file: "build/$packageName" + ".xml")
    }
    def mpackageFilename = "build/${packageName}.mpackage"
    e.echo("Zipping package contents into mpackage file $mpackageFilename")
    ant.zip(baseDir: 'build/tmp', destFile: mpackageFilename)
    if (outputFile) {
      def cwd = System.properties['user.dir']
      def outFile = new File('./.output')
      def line = "{ \"name\": \"$packageName\", \"path\": \"/$mpackageFilename\" }\n"
      outFile.newWriter().withWriter { w ->
        w << line
      }
    }
    e.echo("Build completed successfully!")
    System.exit(0)
  }

  static void parse(String filename) {
        File xmlFile = new File(filename)
        if (!xmlFile.exists()) {
            println "The file '$filename' does not exist."
            return
        }

        try {
            String xmlContent = xmlFile.text

            def basefile = filename.tokenize("/")[-1]
            def basename = basefile.tokenize(".")[-2]

            Parser.generate(Parser.parse(xmlContent), ".", basename)
            println "Successfully parsed and processed XML file: $filename"
        } catch (Exception e) {
            println "An error occurred while reading or parsing the XML file: ${e.message}"
        }
  }
}
