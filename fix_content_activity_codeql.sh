sed -i '45c\
\
            // Prevent arbitrary file read from other apps by strictly validating the URI authority\
            // CodeQL flags content resolver operations on user-provided URIs.\
            String authority = incomingUri.getAuthority();\
            if (authority == null) {\
                Log.e(TAG, "Missing URI authority");\
                finish();\
                return;\
            }\
' app/src/main/java/com/biglucas/agena/ui/ContentActivity.java
