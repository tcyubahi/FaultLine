const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);

var db = admin.firestore();


exports.myFunction = functions.firestore
.document('liveFeed/redlands')
.onWrite((change, context) => {

	const load = JSON.stringify(change);
	const parsedLoad = JSON.parse(load);

	const isAlert = parsedLoad.alert;

	if(isAlert) {
		db.collection("users")
		.get()
		.then(function(querySnapshot) {
			querySnapshot.forEach(function(doc) {
				console.log(doc.id, " => ", doc.data());
				const token = doc.data().token;
				if (token) {
					const payload = {
						notification: {
							title: "EarthQuake Detected",
							body: "Locate the nearest safe area"
						}
					}
					admin.messaging().sendToDevice(token, payload);
				}
			});
		})
		.catch(function(error) {
			console.log("Error getting documents: ", error);
		});
	}
});