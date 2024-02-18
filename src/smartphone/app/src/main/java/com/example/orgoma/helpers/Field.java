package com.example.orgoma.helpers;

import com.google.android.gms.maps.model.Polygon;

/* This class represent a field, which is a polygon and has some important attributes, like if it is organic and if it is sprayed.
*  We mainly use it in the ArrayList loaded_fields in the class MainActivity. We do that because if the ArrayList was of type Polygon then
*  we will have to get from the database the attributes organic_farming and sprayed, that will add loading time in the app, we don't want that.
*  This class also help us to implement more efficiently the function calculateDistance(...) in the class MainActivity. */
public class Field {
    private Polygon polygon;
    private String organic_farming;

    private String sprayed;


    public Field(Polygon polygon, String organic_farming, String sprayed) {
        this.polygon = polygon;
        this.organic_farming = organic_farming;
        this.sprayed = sprayed;
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public String getOrganic_farming() {
        return organic_farming;
    }

    public String getSprayed() {
        return sprayed;
    }

    public void setSprayed(String sprayed) {
        this.sprayed = sprayed;
    }
}
