# Ultimate Tic-Tac-Toe

This is a sample project to demonstrate the integration between Play, Akka, and Scala.js, in order to implement a game of *ultimate tic-tac-toe*.

The demo for this application can be found at [ultimate-tictactoe.herokuapp.com](ultimate-tictactoe.herokuapp.com).

The game mechanics are best described in the following [blog post](https://mathwithbaddrawings.com/2013/06/16/ultimate-tic-tac-toe/) on *Math With Bad Drawings*. Whereas regular tic-tac-toe is a boring game that involves an inevitable stalemate, *Ultimate Tic-Tac-Toe* brings a little excitement to the world of tic-tac-toe and forces strategic thinking.

![](https://canvas-files-prod.s3.amazonaws.com/uploads/cc6731a7-1152-4bce-9a14-e66e44985b3c/Screenshot 2016-09-11 19.37.29.png)

## Project Goals

The goal of this sample project is to demonstrate the usage of some key technologies and how they integrate and complement each other:

- Demonstrate concrete usage of advanced Akka features such as [Akka FSM](http://doc.akka.io/docs/akka/current/java/fsm.html) and how they can be leveraged to simplify the handling of state within message-driven systems.
- Leverage [Scala.js](https://www.scala-js.org/) to implement a reactive UI without resorting to complex JavaScript technologies like Angular or React.
 
## Current Status

The following functionality is working:

- The *home page*:
    - Create a new game (by entering your first name)
    - Join a game that has been created by someone else
 
- The *game page*:
    - Keeps track of total number of moves per player.
    - Keeps track of cumulative time of the turns for each player.
    - Win, loss, and tie game statistics.
    - Request a rematch.
 
 
If you would like to contribute to this demo, please view the list of GitHub issues for ideas of functionality that would be nice to have.

## Installing and running locally

### Prerequisites

The easiest way to get started is to download the *Lightbend Activator*, which bundles all dependencies for Scala, Play, Akka, and SBT together.

*(Ignore the fact that the download artefact is called Typesafe Activator.)*

[https://www.lightbend.com/activator/download](https://www.lightbend.com/activator/download)

You can also download the open-source projects separately by visiting each of their home pages.

### Compile, test, run

Once you have downloaded the prerequisites above, you'll need to compile, test, and execute using SBT.

To run, simply type the following to compile:

```sbt clean compile`

Followed by:

```sbt test`

And finally:

```sbt run`

This will launch the server on `port 9000`. In order to play a game, you can open two browser windows and point them to `localhost:9000`.
