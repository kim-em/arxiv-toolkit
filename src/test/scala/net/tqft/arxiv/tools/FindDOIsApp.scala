package net.tqft.arxiv.tools

import net.tqft.util.SeleniumSlurp

object FindDOIsApp extends App {
  for ((id, doi) <- FindDOIs.forAuthor("au:Morrison_Scott")) {
    println(id + " ---> " + doi)
  }

  SeleniumSlurp.quit
}