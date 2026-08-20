package br.com.accessmap.backend.shared.validation;

public class LocationValidator {
    private LocationValidator(){
    }

    public static boolean isValid(double latitude, double longitude){
        return latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude  <= 180;
    }
}
