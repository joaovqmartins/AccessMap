import { useAuth } from '../auth/AuthContext';
import ReviewListScreen from '../screens/ReviewListScreen';

export default function MinhasAvaliacoes() {
  const { user } = useAuth();
  // A rota fica atrás do Stack.Protected de quem está logado; sem usuário não há o que listar
  if (!user) return null;
  return <ReviewListScreen userId={user.id} />;
}
