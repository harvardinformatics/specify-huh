<?xml version='1.0' encoding='ISO-8859-1'?>
<!DOCTYPE helpset PUBLIC "-//Sun Microsystems Inc.//DTD JavaHelp HelpSet Version 2.0//EN"
                         "http://java.sun.com/products/javahelp/helpset_2_0.dtd">

<helpset version="2.0">
	<title>Specify Help</title>
	<maps>
		<homeID></homeID>
		<mapref location="SpecifyHelp.jhm"/>
	</maps>
	<view mergetype="javax.help.UniteAppendMerge">
		<name>TOC</name>
		<label>Table Of Contents</label>
		<type>javax.help.TOCView</type>
		<data>SpecifyHelpTOC.xml</data>
	</view>
	<view mergetype="javax.help.SortMerge">
		<name>Index</name>
		<label>Index</label>
		<type>javax.help.IndexView</type>
		<data>SpecifyHelpIndex.xml</data>
	</view>
	<!--<view>
		<name>Search</name>
		<label>Search</label>
		<type>javax.help.SearchView</type>
		<data engine="com.sun.java.help.search.DefaultSearchEngine">JavaHelpSearch</data>
	</view>-->
	<!--<view mergetype="javax.help.SortMerge">
		<name>glossary</name>
		<label>Glossary</label>
		<type>javax.help.GlossaryView</type>
		<data>SpecifyHelpGlossary.xml</data>
	</view>-->
	<view mergetype="javax.help.NoMerge">
		<name>favorites</name>
		<label>Favorites</label>
		<type>javax.help.FavoritesView</type>
	</view>
</helpset>
