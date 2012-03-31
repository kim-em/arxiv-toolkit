// ==UserScript==
// @name	  Add arXiv links to mathscinet
// @version	  0.1.1
// @namespace	  http://tqft.net/
// @match	  http://www.ams.org/mathscinet*
// @require       http://ajax.googleapis.com/ajax/libs/jquery/1.6.4/jquery.min.js
// @updateURL     https://bitbucket.org/scottmorrison/arxiv-toolkit/raw/tip/src/main/js/mathscinet-extension/mathscinet.user.js
// ==/UserScript==

// if we're running under Greasemonkey, we have to tell jQuery how to make requests.
initXHR();

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

function initXHR() {
	// taken from http://ryangreenberg.com/archives/2010/03/greasemonkey_jquery.php
	if (typeof GM_setValue == "function") { // greasemonkey 
		// Wrapper for GM_xmlhttpRequest
		function GM_XHR() {
		    this.type = null;
		    this.url = null;
		    this.async = null;
		    this.username = null;
		    this.password = null;
		    this.status = null;
		    this.headers = {};
		    this.readyState = null;
		
		    this.open = function(type, url, async, username, password) {
		        this.type = type ? type : null;
		        this.url = url ? url : null;
		        this.async = async ? async : null;
		        this.username = username ? username : null;
		        this.password = password ? password : null;
		        this.readyState = 1;
		    };

		    this.setRequestHeader = function(name, value) {
		        this.headers[name] = value;
		    };

		    this.abort = function() {
		        this.readyState = 0;
		    };

		    this.getResponseHeader = function(name) {
		        return this.headers[name];
		    };

		    this.send = function(data) {
		        this.data = data;
		        var that = this;
		        GM_xmlhttpRequest({
		            method: this.type,
		            url: this.url,
		            headers: this.headers,
		            data: this.data,
		            onload: function(rsp) {
		                // Populate wrapper object with returned data
		                for (k in rsp) {
		                    that[k] = rsp[k];
		                }
		            },
		            onerror: function(rsp) {
		                for (k in rsp) {
		                    that[k] = rsp[k];
		                }
		            },
		            onreadystatechange: function(rsp) {
		                for (k in rsp) {
		                    that[k] = rsp[k];
		                }
		            }
		        });
		    };
		};
		// Once we have this wrapper object, we need to tell jQuery to use it instead of the standard browser XHR:

		$.ajaxSetup({
		    xhr: function(){return new GM_XHR;}
		});
	}
}
