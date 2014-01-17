package asset.pipeline.sass
import asset.pipeline.CacheManager
import asset.pipeline.AssetHelper
import asset.pipeline.AbstractAssetFile
import asset.pipeline.processors.CssProcessor

class SassAssetFile extends AbstractAssetFile {
	static final String contentType = 'text/css'
	static extensions = ['sass','css.sass','css.scss', 'scss']
	static final String compiledExtension = 'css'
	static processors = [SassProcessor,CssProcessor]


	String processedStream(Boolean precompiler) {
		def fileText
		def skipCache = precompiler ?: (!processors || processors.size() == 0)

		if(baseFile?.encoding || encoding) {
			fileText = file?.getText(baseFile?.encoding ? baseFile.encoding : encoding)
		} else {
			fileText = file?.text
		}

		for(processor in processors) {
			def processInstance = processor.newInstance(precompiler)
			fileText = processInstance.process(fileText, this)
		}



		return fileText
	}

	String directiveForLine(String line) {
		line.find(/\*=(.*)/) { fullMatch, directive -> return directive }
	}
}
