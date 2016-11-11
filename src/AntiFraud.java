import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

/**
 * Tries to detect fraudulent transactions.
 * First builds the social graph from batch data. Keeps the graph in memory.
 * To classify transaction at run time, employs Dijkstra's algorithm to find
 * shortest path between two people (denoted by their id). Using path length,
 * classify transaction according to feature definition.
 *
 * Leverages jgrapht library (http://jgrapht.org/) to construct social graph
 * as well as to do graph traversal to find shortest path.
 *
 * In production environment, graph should be constructed once from batch data
 * and then loaded from disk. This implementation doesn't do that.
 * Also, full graph is in memory which may be too large in real world.
 * That can be improved such that graph can be partially on disk and loaded 
 * in memory on demand.
 */
public class AntiFraud {

  private static final String TRUSTED    = "trusted\n";
  private static final String UNVERIFIED = "unverified\n";

  public static void main(String[] args) {

    if (args.length != 5) {
      // basic error checking
      throw new IllegalStateException("Expected exactly five inputs. Found: " + args.length);
    }

    // First create a graph.
    UndirectedGraph<Integer, DefaultEdge> paymentGraph = createPaymentGraph(args[0]);

    if (paymentGraph == null) {
      // Failed to create graph. Nothing to do.
      return;
    }

    Iterator<String> lines = null;
    try {
      lines = Files.lines(Paths.get(args[1])).iterator();
    } catch (IOException e) {
      System.out.println("Can't read stream file: "+ args[1]);
      return;
    }

    if (lines.hasNext()) {
      lines.next(); // first line is header, ignore it.
    } else {
      System.out.println("stream file is empty: "+ args[1]);
      return;
    }

    if (!lines.hasNext()) {
      System.out.println("stream file is empty: "+ args[1]);
      return;
    }

    FileWriter fw1;
    try {
      fw1 = new FileWriter(args[2]);
    } catch (IOException e1) {
      System.out.println("Can't open file to write: " + args[2]);
      return;
    }
    BufferedWriter bw1 = new BufferedWriter(fw1);
    PrintWriter feature1 = new PrintWriter(bw1);

    FileWriter fw2;
    try {
      fw2 = new FileWriter(args[3]);
    } catch (IOException e1) {
      System.out.println("Can't open file to write: " + args[3]);
      feature1.close();
      return;
    }
    BufferedWriter bw2 = new BufferedWriter(fw2);
    PrintWriter feature2 = new PrintWriter(bw2);

    FileWriter fw3;
    try {
      fw3 = new FileWriter(args[4]);
    } catch (IOException e1) {
      System.out.println("Can't open file to write: " + args[4]);
      feature1.close();
      feature2.close();
      return;
    }

    BufferedWriter bw3 = new BufferedWriter(fw3);
    PrintWriter feature3 = new PrintWriter(bw3);

    System.out.println("Begin to classify streaming transactions.");
    long begin = System.currentTimeMillis();
    int classified = 0;

    while (lines.hasNext()) {
      String line = lines.next();
      String[] splitted = line.split(",");
      int pathLen = -1;
      try {
        pathLen = DijkstraShortestPath.findPathBetween(paymentGraph, Integer.parseInt(splitted[1].trim()), Integer.parseInt(splitted[2].trim())).size();
      } catch (Exception e) {
        // If we fail to find shortest path, we assume unverified.
      }
      switch (pathLen) {
        case 0: // transaction to self
        case 1: // transacting to friend
          feature1.write(TRUSTED);
          feature2.write(TRUSTED);
          feature3.write(TRUSTED);
          break;
        case 2: // 1-degree away
          feature1.write(UNVERIFIED);
          feature2.write(TRUSTED);
          feature3.write(TRUSTED);
          break;
        case 3: // 2-degree away
        case 4: // 3-degree away
          feature1.write(UNVERIFIED);
          feature2.write(UNVERIFIED);
          feature3.write(TRUSTED);
          break;
        default: // outside of network
          feature1.write(UNVERIFIED);
          feature2.write(UNVERIFIED);
          feature3.write(UNVERIFIED);
          break;
      }

      classified++;
      if (classified > 0 && classified % 100 == 0) {
        System.out.println("Classified " + classified + " transactions successfully.");
        System.out.println("Time taken in seconds: " + ((System.currentTimeMillis()-begin)/1000));
      }
    }
    feature1.close();
    feature2.close();
    feature3.close();
    System.out.println("Classified " + classified + " transactions successfully.");
    System.out.println("Time taken in seconds: " + ((System.currentTimeMillis()-begin)/1000));
 
  }

  /**
   * @param fileName of batch data to construct social graph
   * @return Fully constructed graph
   */

  private static UndirectedGraph<Integer, DefaultEdge> createPaymentGraph(String fileName) {
      UndirectedGraph<Integer, DefaultEdge> g = new SimpleGraph<>(DefaultEdge.class);

      long begin = System.currentTimeMillis();
      Iterator<String> lines;
      try {
        lines = Files.lines(Paths.get(fileName)).iterator();
      } catch (IOException e) {
        System.out.println("Can't read input file for batch data: "+fileName);
        return null;
      }

      if (lines.hasNext()) {
        lines.next(); // first line is header, ignore it.
      } else {
        System.out.println("batch file is empty: "+ fileName);
        return null;
      }

      if (!lines.hasNext()) {
        System.out.println("batch file is empty: "+ fileName);
        return null;
      }

      // We got something valid to read
      int valid = 0;
      while (lines.hasNext()) {
        String line = lines.next();
        Integer[] ids;
        try {

          ids = getIDs(line);
        } catch (Exception e) {
          // In production code, we would log unparseable lines in
          // well defined format in predefined log file.
          // Here we just ignore them for simplicity.
          continue;
        }
        Integer from = ids[0];
        Integer to = ids[1];
        g.addVertex(from);
        g.addVertex(to);
        g.addEdge(from,to);
        valid++;
      }
      // Always good to benchmark. So, lets note how long we took to construct graph.
      System.out.println("Loaded " + valid + " transactions in graph.");
      System.out.println("Time taken in seconds: " + ((System.currentTimeMillis()-begin)/1000));
      return g;
  }

  // Utility method to parse payment line.
  private static Integer[] getIDs(String line) {
    String[] splitted = line.split(",");
    Integer from = Integer.parseInt(splitted[1].trim());
    Integer to = Integer.parseInt(splitted[2].trim());
    return new Integer[] {from,to};
  }

}

