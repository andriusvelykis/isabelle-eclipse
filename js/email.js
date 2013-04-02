String.prototype.reverse = new Function("return this.split('').reverse().join('')");

function email(user,domain,mailto) {
	return String(mailto?'mailto:':'')+String(user).reverse()+'@'+domain;
}

function unfocus_links() {
	var aTags = document.getElementsByTagName('a');
	for (var i=0; i<aTags.length; i++) {
		aTags[i].onfocus = new Function("this.blur()");
	}
}

window.onload = new Function("unfocus_links()");