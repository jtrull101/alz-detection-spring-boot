import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoadModelComponent } from './components/model/load-model/load-model.component';
import { ModelDetailsComponent } from './components/model/model-details/model-details.component';
import { ModelListComponent } from './components/model/model-list/model-list.component';
import { ModelDeleteComponent } from './components/model/model-delete/model-delete.component';
import { ModelDeleteAllComponent } from './components/model/model-delete-all/model-delete-all.component';

import { ImagePredictComponent } from './components/images/image-predict/image-predict.component';
import { ImagePredictRandomComponent } from './components/images/image-predict-random/image-predict-random.component';
import { ImagePredictRandomCategoryComponent } from './components/images/image-predict-random-category/image-predict-random-category.component';
import { ImagePredictDeleteComponent } from './components/images/image-predict-delete/image-predict-delete.component';

import { PageBodyComponent } from './components/page components/page-body/page-body.component';
import { HomeComponent } from './components/home/home.component';

const routes: Routes = [
    { path: '', component: PageBodyComponent, children: [
      { path: '', component: HomeComponent },
      { path: 'model', component: ModelDetailsComponent },
      { path: 'model/load', component: LoadModelComponent },
      { path: 'model/all', component: ModelListComponent },
      { path: 'model/delete', component: ModelDeleteComponent },
      { path: 'model/delete/all', component: ModelDeleteAllComponent },
      { path: 'model/predict', component: ImagePredictComponent },
      { path: 'model/predict/random', component: ImagePredictRandomComponent },
      { path: 'model/predict/random/:impairment', component: ImagePredictRandomCategoryComponent },
      { path: 'model/predict/delete', component: ImagePredictDeleteComponent }
    ]},
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
