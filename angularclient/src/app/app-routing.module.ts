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

import { MenuBodyComponent } from './components/menu-body/menu-body.component';

const routes: Routes = [
  // maybe redirect to 'welcolme' page instead
  // { component: MenuBodyComponent,
  //   path: '',
  //   children:[
  //     { path: 'model/load', component: LoadModelComponent },
  //     { path: 'model?id=:id', component: ModelDetailsComponent },
  //     { path: 'model/all', component: ModelListComponent },
  //     { path: 'model/delete?id=:id', component: ModelDeleteComponent },
  //     { path: 'model/delete/all', component: ModelDeleteAllComponent },
  //     { path: 'model/:modelId/predict', component: ImagePredictComponent },
  //     { path: 'model/:modelId/predict/random', component: ImagePredictRandomComponent },
  //     { path: 'model/:modelId/predict/random/:impairment', component: ImagePredictRandomCategoryComponent },
  //     { path: 'model/:modelId/predict?id=:id', component: ImagePredictDeleteComponent }
  //   ]}

    { path: 'model/load', component: LoadModelComponent },
    { path: 'model', component: ModelDetailsComponent },
    { path: 'model/all', component: ModelListComponent },
    { path: 'model/delete', component: ModelDeleteComponent },
    { path: 'model/delete/all', component: ModelDeleteAllComponent },
    { path: 'model/:modelId/predict', component: ImagePredictComponent },
    { path: 'model/:modelId/predict/random', component: ImagePredictRandomComponent },
    { path: 'model/:modelId/predict/random/:impairment', component: ImagePredictRandomCategoryComponent },
    { path: 'model/:modelId/predict/delete', component: ImagePredictDeleteComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
