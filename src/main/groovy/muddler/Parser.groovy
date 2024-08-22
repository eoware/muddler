package muddler
import muddler.Echo
import muddler.mudlet.packages.AliasPackage
import muddler.mudlet.packages.KeyPackage
import muddler.mudlet.packages.TriggerPackage
import muddler.mudlet.packages.TimerPackage
import muddler.mudlet.packages.ScriptPackage
import muddler.mudlet.packages.Package
import groovy.xml.XmlSlurper
import groovy.xml.XmlUtil
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
import groovy.io.FileType

class Parser {

    static List<Package> parse(String rawXml) {

        def xml = rawXml.replaceAll("<!DOCTYPE[^>]*>", "")

        def xmlSlurper = new XmlSlurper().parseText(xml)
        def mudletPackageNode = xmlSlurper
        List<Package> packagesList = []

        mudletPackageNode.children().each { childNodeXml ->

            def childNode = XmlUtil.serialize(childNodeXml)

            (new Echo()).echo("Reading package: " + childNodeXml.name())

            switch (childNodeXml.name()) {
                case 'AliasPackage':
                    packagesList.add(AliasPackage.fromXml(childNode))
                    break
                case 'KeyPackage':
                    packagesList.add(KeyPackage.fromXml(childNode))
                    break
                case 'TriggerPackage':
                    packagesList.add(TriggerPackage.fromXml(childNode))
                    break
                case 'ScriptPackage':
                    packagesList.add(ScriptPackage.fromXml(childNode))
                    break
                case 'TimerPackage':
                    packagesList.add(TimerPackage.fromXml(childNode))
                    break
            }
        }

        return packagesList
    }

    static void generate(List<Package> packagesList, String baseDirectoryPath, String mudletPackageName ) {

        packagesList.each { pkg ->
            generatePackageFiles(baseDirectoryPath, pkg, mudletPackageName)
        }
    }

    private static void generatePackageFiles(String basePath, Package pkg, String mudletPackageName) {
        String packageType = pkg.getClass().getSimpleName().toLowerCase().replace('package', '')

        def packagePlural = (packageType + "s").replaceAll("aliass", "aliases")

        def separator = File.separator // Gets the system-dependent name-separator character as a string
        def baseDirectoryPath = basePath + separator + "src" + separator + packagePlural //+ separator + mudletPackageName

        File packageDir = new File(baseDirectoryPath)
        packageDir.mkdirs()

        pkg.children.each { item ->
            generateItemFiles(packageDir, item)
        }
    }

    private static void generateItemFiles(File packageDir, def item) {

        def e = new Echo()

        String itemType = item.getClass().getSimpleName().toLowerCase()

        def itemPlural = (itemType + "s").replaceAll("aliass", "aliases")

        if (item.isFolder == "yes") {
            File groupDir = new File(packageDir, item.name)
            groupDir.mkdirs()
            File jsonFile = new File( groupDir, "group.json" )

            item.children.each { childItem ->
                generateItemFiles(groupDir, childItem)
            }
            setJsonInFile(jsonFile, item)
        } else {

            if(item.script) {
                File itemFile = new File(packageDir, "${item.name}.lua")
                itemFile.text = item.script
                item.script = ""
            }
            
            File jsonFile = new File( packageDir, itemPlural + ".json" )

            addJsonToFile( jsonFile, item )
        }
    }

    private static void setJsonInFile( File file, def item )
    {
        item.children = null

        file.text = new JsonBuilder(item).toPrettyString()
    }

    private static void addJsonToFile( File file, def item)
    {
        def itemList

        def jsonSlurper = new JsonSlurper()

        if (!file.exists()) {
            // If the file doesn't exist, initialize it with an empty list
            itemList = []
            file.text = new JsonBuilder(itemList).toString()
        } else {
            // If the file exists, read its content and parse it
            def fileContent = file.text
            itemList = jsonSlurper.parseText(fileContent)
        }

        def itemJson = new JsonBuilder(item).toPrettyString()

        def newItem = jsonSlurper.parseText(itemJson)

        itemList << newItem

        file.text = new JsonBuilder(itemList).toPrettyString()
    }
}
