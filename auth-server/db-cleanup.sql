-- delete tokens
--	1. which have expired and not been introspected
--	2. have not been introspected within an hour of issue time

DELETE FROM token WHERE ((NOW() > expiry) OR (EXTRACT(EPOCH FROM (NOW() - issued_at)) > 3600)) AND introspected = false 
