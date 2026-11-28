import { NavLink } from 'react-router-dom';
import { LayoutDashboard, Users, FolderKanban, TestTube2, Database, ClipboardList, BarChart3, FileText, Rocket, Plug, GitBranch, ScrollText, Settings, LogOut, Smartphone } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useAuth } from '@/context/AuthContext';

const navigation = [
  { name: 'Dashboard', href: '/', icon: LayoutDashboard },
  { name: 'Teams', href: '/teams', icon: Users },
  { name: 'Projects', href: '/projects', icon: FolderKanban },
  { name: 'Test Runs', href: '/test-runs', icon: TestTube2 },
  { name: 'Maestro', href: '/maestro', icon: Smartphone },
  { name: 'Test Data', href: '/test-data', icon: Database },
  { name: 'QA Requests', href: '/qa-requests', icon: ClipboardList },
  { name: 'QA Dashboard', href: '/qa-dashboard', icon: BarChart3 },
  { name: 'Documentation', href: '/documentation', icon: FileText },
  { name: 'Releases', href: '/releases', icon: Rocket },
  { name: 'Integrations', href: '/integrations', icon: Plug },
  { name: 'Contributors', href: '/contributors', icon: GitBranch },
  { name: 'Audit Trail', href: '/audit', icon: ScrollText },
  { name: 'Users', href: '/settings/users', icon: Settings },
];

export function Sidebar() {
  const { auth, logout } = useAuth();

  return (
    <aside className="w-64 border-r border-[var(--oqh-border)] bg-white min-h-screen flex flex-col">
      <div className="px-4 py-5 border-b border-[var(--oqh-border)]">
        <img src="/sidebar-logo.svg" alt="" className="h-14 w-auto" />
        <h1 className="sr-only">Open Quality Hub</h1>
        <p className="text-[11px] text-[var(--oqh-text-muted)] mt-0.5 pl-0.5">Quality Engineering Platform</p>
      </div>
      <nav className="px-3 py-4 space-y-0.5 flex-1" aria-label="Main navigation">
        {navigation.map((item) => (
          <NavLink
            key={item.name}
            to={item.href}
            end={item.href === '/'}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 px-3 py-2.5 rounded-[10px] text-[13px] font-medium transition-colors',
                isActive
                  ? 'bg-[var(--oqh-active-bg)] text-[#105EC7] font-bold'
                  : 'text-[#48586E] hover:bg-gray-50 hover:text-[var(--oqh-text)]'
              )
            }
          >
            {({ isActive }) => (
              <>
                <item.icon className={cn('h-[19px] w-[19px]', isActive ? 'text-[var(--oqh-blue)]' : 'text-[#64758A]')} aria-hidden="true" />
                {item.name}
              </>
            )}
          </NavLink>
        ))}
      </nav>
      {auth && (
        <div className="p-4 border-t border-[var(--oqh-border)]">
          <div className="flex items-center justify-between">
            <div className="min-w-0">
              <p className="text-sm font-medium text-[var(--oqh-text)] truncate">{auth.displayName}</p>
              <p className="text-xs text-[var(--oqh-text-muted)] truncate">{auth.email}</p>
            </div>
            <button
              onClick={logout}
              className="p-2 text-[#64758A] hover:text-[var(--oqh-text)] rounded-lg hover:bg-gray-100 transition-colors"
              aria-label="Sign out"
            >
              <LogOut className="h-4 w-4" aria-hidden="true" />
            </button>
          </div>
        </div>
      )}
    </aside>
  );
}
