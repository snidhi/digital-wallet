# digital-wallet
Digital wallet to identify fraudulent transactions. 
 

Tries to detect fraudulent transactions.
First builds the social graph from batch data. Keeps the graph in memory.
To classify transaction at run time, employs Dijkstra's algorithm to find shortest path between two people (denoted by their id). 
Using path length, classify transaction according to feature definition.

In production environment, graph should be constructed once from batch data
and then loaded from disk. This implementation doesn't do that.
Also, full graph is in memory which may be too large in real world.
That can be improved such that graph can be partially on disk and loaded 
in memory on demand.

Leverages jgrapht library (http://jgrapht.org/) to construct social graph as well as to do graph traversal to find shortest path.
For convenience jar for library is provided in repo, so no additional step
is needed to run.
