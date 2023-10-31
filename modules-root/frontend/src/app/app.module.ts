import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LoadModelComponent } from './components/model/load-model/load-model.component';
import { ModelDetailsComponent } from './components/model/model-details/model-details.component';
import { ModelListComponent } from './components/model/model-list/model-list.component';
import { ModelDeleteComponent } from './components/model/model-delete/model-delete.component';
import { ModelDeleteAllComponent } from './components/model/model-delete-all/model-delete-all.component';
import { ImagePredictComponent } from './components/images/image-predict/image-predict.component';
import { ImagePredictRandomComponent } from './components/images/image-predict-random/image-predict-random.component';
import { ImagePredictRandomCategoryComponent } from './components/images/image-predict-random-category/image-predict-random-category.component';
import { ImageDetailsComponent } from './components/images/image-details/image-details.component';
import { ImagePredictDeleteComponent } from './components/images/image-predict-delete/image-predict-delete.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MenuBodyComponent } from './components/page components/menu-body/menu-body.component';
import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { ModelService } from './services/model.service';
import { ImagePredictionService } from './services/image-prediction.service';
import { PageBodyComponent } from './components/page components/page-body/page-body.component';
import { HomeComponent } from './components/home/home.component';
import { ModelPlotComponent } from './components/model/model-plot/model-plot.component';


@NgModule({
  declarations: [
    AppComponent,
    LoadModelComponent,
    ModelDetailsComponent,
    ModelListComponent,
    ModelDeleteComponent,
    ModelDeleteAllComponent,
    ImagePredictComponent,
    ImagePredictRandomComponent,
    ImagePredictRandomCategoryComponent,
    ImagePredictDeleteComponent,
    ImageDetailsComponent,
    MenuBodyComponent,
    PageBodyComponent,
    HomeComponent,
    ModelPlotComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    FormsModule,
    HttpClientModule,
    BrowserAnimationsModule,
    MatMenuModule,
    MatButtonModule,
  ],
  providers: [
    ModelService,
    ImagePredictionService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
