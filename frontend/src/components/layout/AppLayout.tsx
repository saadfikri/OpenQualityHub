import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { SearchBar } from './SearchBar';
import { NotificationBell } from './NotificationBell';

export function AppLayout() {
  return (
    <div className="flex min-h-screen bg-[var(--oqh-page)]">
      <Sidebar />
      <div className="flex-1 flex flex-col">
        <header className="h-14 border-b border-[var(--oqh-border)] bg-white flex items-center justify-between px-8 shrink-0">
          <SearchBar />
          <NotificationBell />
        </header>
        <main className="flex-1 p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
