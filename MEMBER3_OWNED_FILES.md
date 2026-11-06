# Member 3 — Search & Filter & Transaction Management
Owned / primary files (folder structure preserved for GitHub).

## Backend (search & filter)
- controller/CarController.java — GET search endpoint
- service/CarService.java — search(...) filters & sort
- domain/Car.java, CarStatus.java, VehicleType.java (used by filters)
- repository/CarRepository.java

## Data used by search
- data/cars.json, data/images/*, sample-data/cars.json

## Frontend (browse / filter / results)
- static/app.js, app.css, theme.css, index.html, fx.js
  (your sections: search panel, filters, inventory grid, explore flow)

## Note
Shared with Member 2 on CarService / CarController — you own search methods; they own CRUD.
