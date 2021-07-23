Original App Design Project README
===

# ARt Gal

## Table of Contents
1. [Overview](#Overview)
1. [Product Spec](#Product-Spec)
1. [Wireframes](#Wireframes)
2. [Schema](#Schema)

## Overview
### Description
ARt Gal is an Android and multisharing AR app that allows users to detect and augment 2D images in the their environment, such as posters or physical artwork. Users can also attach augmented anchors of their own onto 2D representations and place custom AR effects that other users can see and experience.

### App Evaluation
- **Category:** Photo / Video, Multiplayer AR, Social
- **Mobile:** Uses camera, mobile first experience
- **Story:** Enhances the user's interaction by bringing the art around them to life through various AR effects that other users want them to see.
- **Market:** Anyone that can appreciate art will find themselves envoloped.
- **Habit:** Users can explore the art outlets in their communities with more dimension. 
- **Scope:** Will start off in concentrated regions, most likely in art spaces, educational institutions, and so forth. This has the potential to expand into a larger scope if more users utilize and share this technology. 

## Product Spec

### 1. User Stories (Required and Optional)

**Required Must-have Stories**

* User can create a new account
* User can login
* User can logout
* User can add a new marker
* User can view marker details
* User can remove their markers
* User can view their current uploaded markers
* User can view rendered AR object over recognized marker through camera view


**Optional Nice-to-have Stories**

* User can find markers near them on a map
* User can edit a marker
* User can view number of likes on a marker
* User can like and favorite markers 
* User can view their favorited markers
* User capture a picture of the AR object being displayed

### 2. Screen Archetypes

* Login Screen
   * User can login
* Create Account Screen
   * User can create a new account
* Add/Edit Marker Screen
    * User can add a new marker
    * User can edit a marker (Optional)
* AR View Screen (LAUNCH)
    * User can view rendered AR objects through camera view
    * User can view number of likes on a marker (Optional)
    * User can like and favorite markers (Optional)
    * User capture a picture of the AR object being displayed (Optional)
* Favorite Markers Screen
    * User can view their favorited markers (Optional)
    * User can like and favorite markers (Optional)
* Home Screen
    * User can logout
* Uploaded Markers Screen
    * User can remove their markers
    * User can view their current uploaded markers
* Marker Map Screen
    * User can find markers near them on a map (Optional)
    * User can view number of likes on a marker (Optional)
* Marker Details
    * User can view marker details

### 3. Navigation

**Tab Navigation** (Tab to Screen)

* Home Dashboard
* Open AR View
* Uploaded Markers
* Favorited Markers
* Marker Map

**Flow Navigation** (Screen to Screen)

* Login Screen
   * => Home Screen
* Create Account Screen
   * => Home Screen
* Add Marker Screen
    * => Uploaded Markers Screen (after you finish creating the marker)
* AR View Screen
    * => Add Marker Screen
    * => Marker Details
* Favorite Markers Screen
    * => Marker Details
* Home Screen
    * => Login Screen
    * => Add Marker Screen
* Uploaded Markers Screen
    * => Marker Details
    * => Add Marker Screen
* Marker Map Screen
    * => Add Marker Screen
    * => Marker Details
* Marker Details
    * => Marker Map
    * => Uploaded Markers Screen
    * => Favorited Markers Screen
    * => AR View Screen
 
## Wireframes
![](https://i.imgur.com/ezlmLqX.png)


## Schema 
### Models
#### User
| Property   | Type   | Description |
| ---------- | ------ | ----------- |
| DocumentId | String | Unique id for the user document (default field) |
| email | String | Email used to log in into user acccount through Firebase Authentication |
| username   | String | Username handle used to log into user account |
| name       | Map    | Contains two keys respectively storing in their values the first (fName) and last name (lName) of the user |
| password   | String | Password used to log in into user account through Firebase Authentication |
| markers    | Cloud Firestore Reference | Stores a reference to the markers subcollection |
| createdAt  | Date and time | Date when user document is created |
| updatedAt  | Date and time | Date when user document is last updated |

#### Marker
| Property | Type | Description |
| -------- | -------- | -------- |
| DocumentId | String | Unique id for the marker document (default field)|
| title    |   String |Name of the marker given by user|
| description| String |Marker description given by user|
| user     | Cloud Firestore Reference |Stores a reference to the user document|
| markerImg| Map |Contains two keys respectively storing in the values of the uri and file name (fileName) of the reference image |
| augmentedObj|Map | Contains two keys respectively storing in the values of the uri and file name (fileName) of the 3D model asset |
| likesCount |Number | Number of likes for the marker|
| createdAt|Date and time|Date when marker document is createdDate when marker document is created|
| updatedAt|Date and time|Date when marker document is last updated|


### Networking
- [Add list of network requests by screen ]
- [Create basic snippets for each Parse network request]
- [OPTIONAL: List endpoints if using existing API such as Yelp]
