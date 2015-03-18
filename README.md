# J-USE for Android
#### (model-driven tool for generating Android BIS apps)

This project was developed within the Software Systems Engineering group at the ISTAR Research Center at the ISCTE-IUL university in Lisbon, Portugal.


## Introduction
This tool allows the automatic generation of business information systems (BIS) apps for Android devices. Those apps include graphical user interfaces (GUIs) and persist data on a open-source object-oriented database (Versant's DB4Objects). Apps are specified with annotated UML class diagrams that are valided with the [USE (UML Specification Environment)](http://sourceforge.net/apps/mediawiki/useocl) from Bremen University. 

Generated apps use our model-driven approach for user navigation. The latter allows traversing GUI screens (each corresponding to a domain entity) by using the semantic links that match the associations and cardinalities among the conceptual domain entities, as expressed in the model.

Our model-driven generative approach scaffolds the production of flexible Android GUIs, suitable for different displays, in terms of size, orientation and resolution.

## Code generator
The generated code is organized in several layers: business, presentation, view-model and persistence layers, that will be described herein.

### Business Layer
This layer includes a Java class for each UML class in the domain model, holding the same name. Each generated class has one private attribute for every attribute and for every association in the domain model. Besides, it contains public constructors, selectors and modifiers for all attributes and associations (one to one, one to many, many to many, association class to their members and vice-versa) in the domain model. Object serializers and comparators are provided as well. The syntax of the generated code follows OCL naming conventions. Chosen Java collection types (`Set`/`HashSet`, `List`/`ArrayList`, `SortedSet`/`TreeSet` and `Queue`/`ArrayDeque`) match closely the ones found in OCL (`Set`, `Bag`, `OrderedSet` and `Sequence`). A public static `allInstances()` selector allows retrieving all instances of this class from the object oriented database.

### Presentation Layer
This layer includes the required folders to render the GUI in different display sizes and resolutions, as required by the diversity of available Android devices, from smart phones to tablets. Those folders hold the XML files used to represent the GUI for each domain class, the manifest XML and all other required configuration files.

### View-Model Layer
This layer includes all the required activities and fragments to represent and dinamically control each domain class in the model.

### Persistence Layer
This layer provides a façade to interface [Database for Objects (DB4O)](http://supportservices.actian.com/versant/default.html), an open source object database engine. This façade provides basic CRUD (create, read, update and delete) capabilities, along with cleanup and lookup ones.

## More Info / Citations
For more information on the internal details of the JUSE4Android tool or for citation purposes, please refer to:
  * Luís Pires da Silva, Fernando Brito e Abreu, “[Model-Driven GUI Generation and Navigation for Android BIS Apps](https://sites.google.com/site/quasarresearchgroup/ouractivity/publications)”, proceedings of the [2nd International Conference on Model-Driven Engineering and Software Development (MODELSWARD’2014)](http://www.modelsward.org/?y=2014), Lisbon, Portugal, 7-9 January 2014.
  * Luís Pires da Silva,	"[A Model-Driven Approach to Generative Programming for Mobile Devices](https://sites.google.com/site/quasarresearchgroup/ouractivity/dissertations)", MSc dissertation, (supervised by Fernando Brito e Abreu), [University Institute of Lisbon (ISCTE-IUL)](http://www.iscte-iul.pt/), December 2013.


### Introduction video
Small video showing the generation process and the resulting generated app. Thereafter is shown in the Android emulator the functionalities of the generated app.  

[check video](https://www.youtube.com/watch?v=pJ2pVSP5_FY&feature=youtu.be&vq=hd720)

(consider watching the video in hd)

# Notes
## Installation
In order to run JUSE4Android java is required. It can be used as a java project or it can be run as a standalone tool. For the latter it is provided an executable jar, found in the folder. this zip file also contains already prepared examples. [executable](https://github.com/LuisPiresSilva/juse4android/tree/master/executable).

## Requirements
 * the model must be specified in the USE(.use) syntax and this file must be supplied.
 * the path target path must also be supplied.
 * both android and server projects names must be supplied if the model does not have a name.
 * both database access user and password fields must be supplied.

all other fields can be left in blank.

## How to use
(contact us if you experience any problems)
