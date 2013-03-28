package net.tqft.arxiv.tools

object FindDOIsApp extends App {
	for((id, doi) <- FindDOIs.forAuthor("au:Morrison_Scott")) {
	  println(id + " ---> " + doi)
	}
}