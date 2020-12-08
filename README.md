# Travelling-salesman
This project aims at resolving 4 variants of the [travelling salesman problem](https://en.wikipedia.org/wiki/Travelling_salesman_problem), a famous NP-Hard problem.

## 1 - Rabbits Intro
Short Introduction to the simulation platform, simulation a rabbit population VS grass population.


<a href="./1-rabbits-intro/gardoni-dubach-in/doc/263557-271088-in.pdf" type="application/pdf">Report PDF</a>

[lol](./1-rabbits-intro/gardoni-dubach-in/doc/263557-271088-in.pdf)
## 2 - Reactive
The intelligent agent represents a unique vehicle, trying to maximize profit. He travels trough a country looking for package to be delivered from town A to town B.
When he package appears, the reward for delivering is communicated and the agents will choose whether he wants to deliver it.

The solution is obtained by training Q-table offline (only the task probably of apparition is known), then blindly applying the table online.

## 3 - Centralized
The intelligent agent represents a vehicle. It knows in advance the list of task that have to be delivered, and while taking into account the weight of each task and it's own capacity, has to find the best way of delivering all those package.

## 4 - Deliberative
The IA represents a company, having multiple deferent vehicle (some big vehicle, which consume a lot, some more economical small vehicle).

It knows in advance the list of task that package that have to be delivered, and while taking into account the weight of each, has to find the best way of delivering all those package.

## 5 – Market 
The IA represent a company, having multiple different vehicles. 
It also have another concurrent Company.

Clients wants to deliver package, and ask (one after the other) the 2 companies the make offers about them.
The company giving the smallest offer win the client.

the core of this project is:
 - Estimating the energy needed to deliver to package (for example if we already have a package with the same destination, not much additional energy is needed ? )
 - Infer a bid from the needed energy
 - Trying to model the opponent in order to maximize our profit while still getting the client.



The project was successfully done in close collaboration with Marcel Dubach, for the EPFL master class *CS-430 Intelligent agents*

