import { OverviewPageComponent } from './pages/overview-page/overview-page.component';
import { NotFoundPageComponent } from './pages/not-found-page/not-found-page.component';
import { BlogPageComponent } from './pages/blog-page/blog-page.component';
import { EditPageComponent } from './pages/edit-page/edit-page.component';
import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: 'edit/:id', component: EditPageComponent },
  { path: 'edit', component: EditPageComponent },
  { path: ':id', component: BlogPageComponent },
  { path: '', component: OverviewPageComponent },
  { path: '**', component: NotFoundPageComponent },
];
