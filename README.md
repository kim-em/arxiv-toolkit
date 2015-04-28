This repository contains a disorganized collection of code for interacting with the arXiv, Scopus, Web of Science, MathSciNet, and Google Scholar.

Example use
===========

Producing all the text citations for an given article on Scopus:

    ./sbt "test:run-main net.tqft.scopus.TextCitationsFromIdentifierApp 2-s2.0-84867097056" | grep -v "\["

 (Here "2-s2.0-84867097056" is a typical Scopus article identifier. The grep command just cleans out junk from sbt, the scala build tool.)

 Output:

    The classification of subfactors of index at most 5, Jones, V.F.R, Morrison, S, Snyder, N
    Principal graph stability and the jellyfish algorithm, Bigelow, S, Penneys, D
    Tensor C*-categories arising as bimodule categories of II1 factors, Falguières, S, Raum, S
    A Planar Calculus for Infinite Index Subfactors, Penneys, D
    Quadratic tangles in planar algebras, Jones, V.F.R.
    Non-cyclotomic fusion categories, Morrison, S, Snyder, N
    Subfactors of Index Less Than 5, Part 3: Quadruple Points, Izumi, M, Jones, V.F.R, Morrison, S, Snyder, N
    Subfactors of Index less than 5, Part 1: The Principal Graph Odometer, Morrison, S, Snyder, N
    Subfactors of index less than 5, part 2: Triple points, Morrison, S, Penneys, D, Peters, E, Snyder, N
    Subfactors of index less than 5, part 4: Vines, Penneys, D., Tener, J.E.
    Fusion rules on a parametrized series of graphs, Asaeda, M, Haagerup, U
    The Exoticness and Realisability of Twisted Haagerup-Izumi Modular Data, Evans, D.E, Gannon, T
    Cyclotomic Integers, Fusion Categories, and Subfactors, Calegari, F, Morrison, S, Snyder, N
    Automorphisms of the bipartite graph planar algebra, Burstein, R.D.
    On the Jones index values for conformal subnets, Carpi, S, Kawahigashi, Y, Longo, R
    From operator algebras to superconformal field theory, Kawahigashi, Y.