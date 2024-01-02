import { Injectable } from '@angular/core';
import { BehaviorSubject, filter } from 'rxjs';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root',
})
export class LoadingService {
  #initialLoading = true;
  #tokens = new Set<number>();
  #loadingState = new BehaviorSubject(true);
  public readonly loadingState$ = this.#loadingState.asObservable();

  constructor(private readonly router: Router) {
    router.events.pipe(filter((e) => e.type === 0)).subscribe(() => this.navigate());
  }

  loadedPage(token?: number) {
    this.#initialLoading = false;
    if (token) {
      this.end(token);
    }
    this.update();
  }

  start(): number {
    const token = Math.random();
    this.#tokens.add(token);
    this.update();
    return token;
  }

  end(token: number) {
    this.#tokens.delete(token);
    this.update();
  }

  private navigate() {
    this.#initialLoading = true;
    this.update();
  }

  private update() {
    const nextState = this.getNextState();
    if (nextState === this.#loadingState.value) {
      return;
    }
    this.#loadingState.next(nextState);
  }

  private getNextState(): boolean {
    if (this.#initialLoading) {
      return true;
    }
    return this.#tokens.size > 0;
  }
}
