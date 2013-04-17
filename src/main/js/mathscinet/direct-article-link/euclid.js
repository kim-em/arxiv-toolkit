console.log("We're at: " + location.href);
location.href = $("div.download-section-div a:contains('PDF File')").first().attr('href');