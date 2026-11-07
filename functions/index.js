
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");

initializeApp();

const db = getFirestore();

exports.deleteClassroom = onCall(
    {
        timeoutSeconds: 540,
        memory: "1GiB"
    },
    async (request) => {

        if (!request.auth) {
            throw new HttpsError(
                "unauthenticated",
                "Please login first."
            );
        }

        const uid = request.auth.uid;
        const classroomId = request.data?.classroomId;

        if (
            typeof classroomId !== "string" ||
            classroomId.trim().length === 0
        ) {
            throw new HttpsError(
                "invalid-argument",
                "Invalid classroom ID."
            );
        }

        const id = classroomId.trim();

        const userRef = db.collection("users").doc(uid);
        const classroomRef = db.collection("classrooms").doc(id);

        const [userDoc, classroomDoc] = await Promise.all([
            userRef.get(),
            classroomRef.get()
        ]);

        if (!userDoc.exists || userDoc.get("role") !== "TEACHER") {
            throw new HttpsError(
                "permission-denied",
                "Only teachers can delete classrooms."
            );
        }

        if (!classroomDoc.exists) {
            throw new HttpsError(
                "not-found",
                "Classroom not found."
            );
        }

        if (classroomDoc.get("teacherId") !== uid) {
            throw new HttpsError(
                "permission-denied",
                "You can only delete your own classroom."
            );
        }

        const collections = [
            "classroom_members",
            "join_requests",
            "attendance_sessions",
            "attendance_records",
            "assessments",
            "student_marks",
            "class_schedules",
            "notices",
            "personal_notes"
        ];

        try {
            for (const collectionName of collections) {

                const collectionRef = db.collection(collectionName);

                while (true) {
                    const snapshot = await collectionRef
                        .where("classroomId", "==", id)
                        .limit(100)
                        .get();

                    if (snapshot.empty) {
                        break;
                    }

                    for (const document of snapshot.docs) {
                        await db.recursiveDelete(document.ref);
                    }
                }
            }

            await db.recursiveDelete(
                db.collection("classroom_lookup").doc(id)
            );

            await db.recursiveDelete(classroomRef);

            return {
                success: true,
                message: "Classroom and associated data deleted successfully."
            };

        } catch (error) {
            console.error("Classroom deletion failed:", error);

            throw new HttpsError(
                "internal",
                "Classroom deletion could not be completed. Check server logs."
            );
        }
    }
);
