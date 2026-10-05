import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { IconButton } from 'react-native-paper';

import { CardDetailsScreen, CardFormScreen, CardListScreen } from '@/features/creditCards';
import { palette } from '@/theme/theme';
import type { CardsStackParamList } from './types';

const Stack = createNativeStackNavigator<CardsStackParamList>();

/**
 * Credit cards stack (HLD §6, §12), nested inside the Cards tab:
 * list → details, plus an add/edit form presented modally.
 */
export function CardsNavigator() {
  return (
    <Stack.Navigator>
      <Stack.Screen
        name="CardList"
        component={CardListScreen}
        options={({ navigation }) => ({
          title: 'Credit cards',
          headerRight: () => (
            <IconButton
              icon="plus"
              iconColor={palette.accent}
              accessibilityLabel="Add card"
              onPress={() => navigation.navigate('CardForm')}
            />
          ),
        })}
      />
      <Stack.Screen
        name="CardDetails"
        component={CardDetailsScreen}
        options={({ navigation, route }) => ({
          title: 'Card details',
          headerRight: () => (
            <IconButton
              icon="pencil"
              iconColor={palette.accent}
              accessibilityLabel="Edit card"
              onPress={() => navigation.navigate('CardForm', { cardId: route.params.cardId })}
            />
          ),
        })}
      />
      <Stack.Screen
        name="CardForm"
        component={CardFormScreen}
        options={({ route }) => ({
          title: route.params?.cardId ? 'Edit card' : 'Add card',
          presentation: 'modal',
        })}
      />
    </Stack.Navigator>
  );
}
