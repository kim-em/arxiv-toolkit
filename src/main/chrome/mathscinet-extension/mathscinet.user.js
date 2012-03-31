// ==UserScript==
// @name	  Add arXiv links to mathscinet
// @version	  0.1.1
// @match	  http://www.ams.org/mathscinet*
// @require       http://ajax.googleapis.com/ajax/libs/jquery/1.6.4/jquery.min.js
// @updateURL     
// ==/UserScript==

var titleElement = $(".headline .title").clone()
// throw out all LaTeX components of the title
// (these may appear twice, due to MathJax processing)
titleElement.find(".MathTeX").remove()
titleElement.find("script").remove()
var title = titleElement.text().replace(/ -/, " ").replace(/\n/, "");
var authors = $(".headline :first-child").nextUntil(".title", 'a[href^="/mathscinet/search/publications"]').map(function() { return $(this).text() })
var MRNumber = $('div.headline strong').first().text();

var authorTerm = authors.map(function() { return 'au:' + this }).get().join(' AND ')
var titleTerm = 'ti:' + title;
var search = 'http://export.arxiv.org/api/query?search_query=' + encodeURIComponent(authorTerm) + ' AND ' + encodeURIComponent(titleTerm);
//alert(search);
$.ajax(search).done(function ( data ) {
	tentative($(data).find("entry id").first().text());
})

function tentative(url) {
	$("a:contains('Make Link')").before($('<a>',{
	    text: 'arXiv?',
	    class: 'tentative',
	    href: url
	}).hide())
	$.ajax(url).done(function (abs) {
		var doc = $(abs);
		var titleElement = doc.find('h1.title').clone();
		titleElement.find('.descriptor').remove();
		var arXivTitle = titleElement.text();
		var arXivAuthors = doc.find('div.authors a').map(function() { return $(this).text() });
		var lastNames = arXivAuthors.map(function() { return this.split(" ").pop() });
		var arXivJournalRef = doc.find('td.jref').text();
		var mref = (arXivJournalRef == '') ?
			('http://www.ams.org/mrlookup?ti=' + encodeURIComponent(arXivTitle) + '&au=' + encodeURIComponent(lastNames.get().join(' and '))) :
			('http://ams.org/mathscinet-mref?ref=' + encodeURIComponent(arXivTitle + '\n' + arXivAuthors.get().join(', ') + '\n' + arXivJournalRef));
//		alert(mref);
		$.ajax(mref).done(function(mrefResult) {
			var MRLink = $(mrefResult).find('a[href^="http://www.ams.org/mathscinet-getitem"]').first().attr('href');
			var mrefMRNumber = MRLink ? 'MR' + MRLink.match(/[\d]+$/) : '';
//			alert(mrefMRNumber);
			if(MRNumber == mrefMRNumber) {
				$('a.tentative').removeClass('tentative').text('arXiv').show();
			} else {
				$('a.tentative').remove();
			}
		})
	})
}
