$base = 'http://localhost:8080'

try {
	$ownerLogin = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType 'application/json' -Body '{"role":"OWNER","mobile":"9999000001","password":"password123"}'
	$ownerToken = $ownerLogin.data.accessToken
	Write-Output ("OWNER_TOKEN(from login):" + $ownerToken)
} catch {
	Write-Output 'Owner login failed, attempting register'
	$ownerReg = Invoke-RestMethod -Method Post -Uri "$base/api/auth/register" -ContentType 'application/json' -Body '{"role":"OWNER","mobile":"9999000001","password":"password123","name":"Owner One","dateOfBirth":"1980-01-01"}'
	$ownerToken = $ownerReg.data.accessToken
	Write-Output ("OWNER_TOKEN(from register):" + $ownerToken)
}

$ownerCreateCmd = "curl.exe -s -X POST '$base/api/owners' -H 'Authorization: Bearer $ownerToken' -F 'name=Owner One' -F 'mobile=9999000001'"
try {
	$me = Invoke-RestMethod -Method Get -Uri "$base/api/auth/me" -Headers @{Authorization="Bearer $ownerToken"}
	if ($me.data.ownerId -eq $null -or $me.data.ownerId -eq '') {
		Write-Output "Creating owner profile via curl"
		Write-Output "Running: $ownerCreateCmd"
		$ownerCreate = iex $ownerCreateCmd
		Write-Output 'OWNER_PROFILE_CREATED'
	} else {
		Write-Output "Owner profile exists: $($me.data.ownerId)"
	}
} catch {
	Write-Output "Failed to check/create owner profile: $_"
}

$rideBody = @{fromLocation='CityA'; toLocation='CityB'; date='2026-08-20'; startTime='09:00:00'; endTime='12:00:00'; price=100.0; carModel='Toyota'; totalSeats=3}
$ride = Invoke-RestMethod -Method Post -Uri "$base/api/rides" -Headers @{Authorization="Bearer $ownerToken"} -Body ($rideBody | ConvertTo-Json -Depth 5) -ContentType 'application/json'
Write-Output ("RIDE_ID:" + $ride.data.id)

$passToken = $null
try {
	$passLogin = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType 'application/json' -Body '{"role":"PASSENGER","mobile":"9999000002","password":"password123"}'
	$passToken = $passLogin.data.accessToken
	Write-Output ("PASS_TOKEN(from login):" + $passToken)
} catch {
	Write-Output 'Passenger login failed, attempting register'
	$passReg = Invoke-RestMethod -Method Post -Uri "$base/api/auth/register" -ContentType 'application/json' -Body '{"role":"PASSENGER","mobile":"9999000002","password":"password123","name":"Passenger One","dateOfBirth":"1990-01-01"}'
	$passToken = $passReg.data.accessToken
	Write-Output ("PASS_TOKEN(from register):" + $passToken)
}

$bookingReq = @{rideId = $ride.data.id; seats=1}
$booking = Invoke-RestMethod -Method Post -Uri "$base/api/bookings" -Headers @{Authorization="Bearer $passToken"} -Body ($bookingReq | ConvertTo-Json -Depth 5) -ContentType 'application/json'
Write-Output ("BOOKING_ID:" + $booking.data.id)
