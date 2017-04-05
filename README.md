SASS/SCSS Compass Grails Asset Pipeline
=======================================
The Grails `sass-asset-pipeline` is a plugin that provides SASS/SCSS support for the asset-pipeline static asset management via libsass. (Libsass was added on version 2.9.0. All previous versions used compass sass)

For more information on how to use asset-pipeline, visit [here](http://www.github.com/bertramdev/asset-pipeline).

*Requires*: Java 8 due to jsass features.

Usage
-----

Simply create files in your standard `assets/stylesheets` folder with extension `.scss` or `.sass`. You also may require other files by using the following requires syntax at the top of each file or the standard SASS import:

```css
/*
*= require test
*= require_self
*= require_tree .
*/

/*Or use this*/
@import 'test'

```

Including Sass files into your GSP files is easy but there are a few things worth mentioning. Say we have a file called `application.scss`. You would include it into your gsp by its compiled extension instead of its original extension. aka, use `.css` instead of `.scss`

```gsp
<head>
  <asset:stylesheet src="application.css"/>
</head>
```


Production
----------
During war build your sass files are compiled into css files. This is all well and good but sometimes you dont want each individual sass file compiled, but rather your main base sass file. It may be best to add a sub folder for those SASS files and exclude it in your precompile config...

Config.groovy:
```groovy
grails.assets.excludes = ["mixins/*.scss"]
```
